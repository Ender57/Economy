package me.clicker.economy.commands;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import me.clicker.economy.*;
import me.clicker.economy.config.Messages;
import me.clicker.economy.storage.SQLiteStorage;

public final class EcoTakeCommand extends AbstractCommand {
    private final HytaleLogger logger;
    private final Economy economy;
    private final SQLiteStorage storage;
    private final RequiredArg<String> playerArg;
    private final RequiredArg<String> amountArg;

    public EcoTakeCommand() {
        super("take", "Take money from a player");

        this.logger = EconomyPlugin.getInstance().getLogger();
        this.economy = EconomyPlugin.getInstance().getEconomy();
        this.storage = EconomyPlugin.getInstance().getStorage();
        playerArg = withRequiredArg("player", "Target player", ArgTypes.STRING);
        amountArg = withRequiredArg("amount", "Amount", ArgTypes.STRING);
    }

    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext ctx) {
        var sender = ctx.sender();

        if (!sender.hasPermission("economy.eco")) {
            sender.sendMessage(ChatColor.toMessage(Messages.eco_no_permission));
            return CompletableFuture.completedFuture(null);
        }

        PlayerRef fromPlayer = null;

        if (ctx.isPlayer()) {
            fromPlayer = Util.getPlayerRef(ctx.sender());
        }

        UUID targetUUID;
        var targetName = this.playerArg.get(ctx);
        var targetPlayer = Universe.get().getPlayerByUsername(targetName, NameMatching.EXACT_IGNORE_CASE);
        var isOffline = targetPlayer == null;

        if (isOffline) {
            try {
                var row = storage.getRow(targetName);

                if (row == null) {
                    sender.sendMessage(ChatColor.toMessage(Messages.player_not_found));
                    return CompletableFuture.completedFuture(null);
                }

                targetName = row.name();
                targetUUID = row.uuid();
            } catch (Exception e) {
                sender.sendMessage(ChatColor.toMessage(Messages.internal_error));
                logger.at(Level.SEVERE).withCause(e).log(fromPlayer == null ? "/eco take failed: from=CONSOLE, targetName=" + targetName : "/eco take failed: fromUUID=" + fromPlayer.getUuid() + ", fromName=" + fromPlayer.getUsername() + ", targetName=" + targetName);
                return CompletableFuture.completedFuture(null);
            }
        } else {
            targetName = targetPlayer.getUsername();
            targetUUID = targetPlayer.getUuid();
        }

        var amountRaw = amountArg.get(ctx);
        double amount;

        try {
            amount = economy.parseFormatted(amountRaw);
        } catch (Exception e) {
            sender.sendMessage(ChatColor.toMessage(Messages.amount_invalid));
            return CompletableFuture.completedFuture(null);
        }

        if (amount <= 0) {
            sender.sendMessage(ChatColor.toMessage(Messages.must_be_positive));
            return CompletableFuture.completedFuture(null);
        }

        double balance;

        try {
            var result = storage.takeIfEnough(targetUUID, targetName, amount);
            balance = result.balance();

            if (!result.success()) {
                if (result.reason() == SQLiteStorage.TakeResult.Reason.NOT_ENOUGH) {
                    sender.sendMessage(ChatColor.toMessage(Messages.eco_take_not_enough.replace("{player}", targetName).replace("{balance}", economy.format(balance, false))));
                } else {
                    sender.sendMessage(ChatColor.toMessage(Messages.player_not_found));
                }

                return CompletableFuture.completedFuture(null);
            }
        } catch (Exception e) {
            sender.sendMessage(ChatColor.toMessage(Messages.internal_error));
            logger.at(Level.SEVERE).withCause(e).log(fromPlayer == null ? "/eco take failed: from=CONSOLE, targetUUID=" + targetUUID + ", targetName=" + targetName + ", amount=" + amount : "/eco take failed: fromUUID=" + fromPlayer.getUuid() + ", fromName=" + fromPlayer.getUsername() + ", targetUUID=" + targetUUID + ", targetName=" + targetName + ", amount=" + amount);
            return CompletableFuture.completedFuture(null);
        }

        sender.sendMessage(ChatColor.toMessage(Messages.eco_take_sender.replace("{player}", targetName).replace("{amount}", economy.format(amount, false)).replace("{balance}", economy.format(balance, false))));

        if (!isOffline) {
            targetPlayer.sendMessage(ChatColor.toMessage(Messages.eco_take_target.replace("{player}", fromPlayer == null ? "CONSOLE" : fromPlayer.getUsername()).replace("{amount}", economy.format(amount, false)).replace("{balance}", economy.format(balance, false))));
        }

        return CompletableFuture.completedFuture(null);
    }
}