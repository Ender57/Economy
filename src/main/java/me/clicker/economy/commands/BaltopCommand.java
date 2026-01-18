package me.clicker.economy.commands;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;

import javax.annotation.Nonnull;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import me.clicker.economy.*;
import me.clicker.economy.config.Config;
import me.clicker.economy.config.Messages;
import me.clicker.economy.storage.SQLiteStorage;

public final class BaltopCommand extends AbstractCommand {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("M/d/yy h:mm a");
    private final HytaleLogger logger;
    private final Economy economy;
    private final SQLiteStorage storage;
    private final OptionalArg<Integer> pageArg;

    public BaltopCommand() {
        super("baltop", "Show top balances");

        this.logger = EconomyPlugin.getInstance().getLogger();
        this.economy = EconomyPlugin.getInstance().getEconomy();
        this.storage = EconomyPlugin.getInstance().getStorage();
        pageArg = withOptionalArg("page", "Page number of the balance leaderboard", ArgTypes.INTEGER);
    }

    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext ctx) {
        var sender = ctx.sender();

        if (!sender.hasPermission("economy.baltop")) {
            sender.sendMessage(ChatColor.toMessage(Messages.baltop_no_permission));
            return CompletableFuture.completedFuture(null);
        }

        PlayerRef fromPlayer = null;

        if (ctx.isPlayer()) {
            fromPlayer = Util.getPlayerRef(ctx.sender());
        }

        var perPage = Config.baltop_page_size;
        int rowCount;

        try {
            rowCount = storage.countRows();
        } catch (Exception e) {
            sender.sendMessage(ChatColor.toMessage(Messages.internal_error));
            logger.at(java.util.logging.Level.SEVERE).withCause(e).log(fromPlayer == null ? "/baltop failed: from=CONSOLE" : "/baltop failed: fromUUID=" + fromPlayer.getUuid() + ", fromName=" + fromPlayer.getUsername());
            return CompletableFuture.completedFuture(null);
        }

        var pages = Math.max(1, (rowCount + perPage - 1) / perPage);
        var page = pageArg.provided(ctx) ? pageArg.get(ctx) : 1;

        if (page < 1) {
            page = 1;
        }

        if (page > pages) {
            page = pages;
        }

        var offset = (page - 1) * perPage;
        List<SQLiteStorage.Row> top;

        try {
            top = storage.getTop(perPage, offset);
        } catch (Exception e) {
            sender.sendMessage(ChatColor.toMessage(Messages.internal_error));
            logger.at(java.util.logging.Level.SEVERE).withCause(e).log(fromPlayer == null ? "/baltop failed: from=CONSOLE, perPage=" + perPage + ", offset=" + offset : "/baltop failed: fromUUID=" + fromPlayer.getUuid() + ", fromName=" + fromPlayer.getUsername() + ", perPage=" + perPage + ", offset=" + offset);
            return CompletableFuture.completedFuture(null);
        }

        double total;

        try {
            total = storage.totalBalance();
        } catch (Exception e) {
            sender.sendMessage(ChatColor.toMessage(Messages.internal_error));
            logger.at(java.util.logging.Level.SEVERE).withCause(e).log(fromPlayer == null ? "/baltop failed: from=CONSOLE" : "/baltop failed: fromUUID=" + fromPlayer.getUuid() + ", fromName=" + fromPlayer.getUsername());
            return CompletableFuture.completedFuture(null);
        }

        var msg = ChatColor.toMessage(Messages.baltop_header.replace("{time}", LocalDateTime.now().format(TS)).replace("{page}", String.valueOf(page)).replace("{pages}", String.valueOf(pages)).replace("{total}", economy.format(total, false)));

        for (var i = 0; i < top.size(); i++) {
            var record = top.get(i);
            msg.insert(ChatColor.toMessage("\n" + Messages.baltop_record.replace("{rank}", String.valueOf(offset + i + 1)).replace("{player}", record.name()).replace("{balance}", economy.format(record.balance(), false))));
        }

        sender.sendMessage(msg);
        return CompletableFuture.completedFuture(null);
    }
}