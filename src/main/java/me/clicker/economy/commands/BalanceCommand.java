package me.clicker.economy.commands;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import me.clicker.economy.*;
import me.clicker.economy.config.Messages;
import me.clicker.economy.storage.SQLiteStorage;

public final class BalanceCommand extends AbstractCommand {
    private final HytaleLogger logger;
    private final Economy economy;
    private final SQLiteStorage storage;
    private final RequiredArg<String> playerArg;

    public BalanceCommand() {
        super("balance", "View your balance or another player's balance");

        this.logger = EconomyPlugin.getInstance().getLogger();
        this.economy = EconomyPlugin.getInstance().getEconomy();
        this.storage = EconomyPlugin.getInstance().getStorage();

        this.addAliases("bal");
        this.addUsageVariant(new AbstractCommand("Show your balance") {
            @Override
            protected CompletableFuture<Void> execute(@Nonnull CommandContext ctx) {
                var sender = ctx.sender();

                if (!sender.hasPermission("economy.balance")) {
                    sender.sendMessage(ChatColor.toMessage(Messages.balance_no_permission));
                    return CompletableFuture.completedFuture(null);
                }

                if (!ctx.isPlayer()) {
                    sender.sendMessage(ChatColor.toMessage(Messages.balance_console_requires_player));
                    return CompletableFuture.completedFuture(null);
                }

                var fromPlayer = Util.getPlayerRef(ctx.sender());

                try {
                    var row = storage.getRow(fromPlayer.getUuid());

                    if (row == null) {
                        sender.sendMessage(ChatColor.toMessage(Messages.player_not_found));
                        return CompletableFuture.completedFuture(null);
                    }

                    sender.sendMessage(ChatColor.toMessage(Messages.balance_self.replace("{balance}", economy.format(row.balance(), false))));
                } catch (Exception e) {
                    sender.sendMessage(ChatColor.toMessage(Messages.internal_error));
                    logger.at(java.util.logging.Level.SEVERE).withCause(e).log("/balance failed: fromUUID=" + fromPlayer.getUuid() + ", fromName=" + fromPlayer.getUsername());
                }

                return CompletableFuture.completedFuture(null);
            }
        });

        playerArg = withRequiredArg("player", "Target player", ArgTypes.STRING);
    }

    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext ctx) {
        var sender = ctx.sender();

        if (!sender.hasPermission("economy.balance.others")) {
            sender.sendMessage(ChatColor.toMessage(Messages.balance_others_no_permission));
            return CompletableFuture.completedFuture(null);
        }

        PlayerRef fromPlayer = null;

        if (ctx.isPlayer()) {
            fromPlayer = Util.getPlayerRef(ctx.sender());
        }

        var targetName = this.playerArg.get(ctx);
        var targetPlayer = Universe.get().getPlayerByUsername(targetName, NameMatching.EXACT_IGNORE_CASE);

        if (targetPlayer != null) {
            targetName = targetPlayer.getUsername();
        }

        try {
            var row = storage.getRow(targetName);

            if (row == null) {
                sender.sendMessage(ChatColor.toMessage(Messages.player_not_found));
                return CompletableFuture.completedFuture(null);
            }

            if (targetPlayer == null) {
                targetName = row.name();
            }

            sender.sendMessage(ChatColor.toMessage(Messages.balance_other.replace("{player}", targetName).replace("{balance}", economy.format(row.balance(), false))));
        } catch (Exception e) {
            sender.sendMessage(ChatColor.toMessage(Messages.internal_error));
            logger.at(Level.SEVERE).withCause(e).log(fromPlayer == null ? "/balance failed: from=CONSOLE, targetName=" + targetName : "/balance failed: fromUUID=" + fromPlayer.getUuid() + ", fromName=" + fromPlayer.getUsername() + ", targetName=" + targetName);
        }

        return CompletableFuture.completedFuture(null);
    }
}