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

import com.hypixel.hytale.server.core.universe.Universe;
import me.clicker.economy.*;
import me.clicker.economy.config.Config;
import me.clicker.economy.config.Messages;
import me.clicker.economy.storage.SQLiteStorage;

public final class PayCommand extends AbstractCommand {
    private final HytaleLogger logger;
    private final Economy economy;
    private final SQLiteStorage storage;
    private final RequiredArg<String> playerArg;
    private final RequiredArg<String> amountArg;

    public PayCommand() {
        super("pay", "Send funds to another player");

        this.logger = EconomyPlugin.getInstance().getLogger();
        this.economy = EconomyPlugin.getInstance().getEconomy();
        this.storage = EconomyPlugin.getInstance().getStorage();
        playerArg = withRequiredArg("player", "Target player", ArgTypes.STRING);
        amountArg = withRequiredArg("amount", "Amount", ArgTypes.STRING);
    }

    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext ctx) {
        var sender = ctx.sender();

        if (!sender.hasPermission("economy.pay")) {
            sender.sendMessage(ChatColor.toMessage(Messages.pay_no_permission));
            return CompletableFuture.completedFuture(null);
        }

        if (!ctx.isPlayer()) {
            sender.sendMessage(ChatColor.toMessage(Messages.pay_player_only));
            return CompletableFuture.completedFuture(null);
        }

        var fromPlayer = Util.getPlayerRef(sender);
        var targetName = this.playerArg.get(ctx);
        var targetPlayer = Universe.get().getPlayerByUsername(targetName, NameMatching.EXACT_IGNORE_CASE);
        var isOffline = targetPlayer == null;
        UUID targetUUID;

        if (isOffline) {
            try {
                if (Config.pay_allow_offline) {
                    var row = storage.getRow(targetName);

                    if (row == null) {
                        sender.sendMessage(ChatColor.toMessage(Messages.player_not_found));
                        return CompletableFuture.completedFuture(null);
                    }

                    targetName = row.name();
                    targetUUID = row.uuid();
                } else {
                    if (storage.exists(targetName)) {
                        sender.sendMessage(ChatColor.toMessage(Messages.pay_pay_offline));
                    } else {
                        sender.sendMessage(ChatColor.toMessage(Messages.player_not_found));
                    }

                    return CompletableFuture.completedFuture(null);
                }
            } catch (Exception e) {
                sender.sendMessage(ChatColor.toMessage(Messages.internal_error));
                logger.at(Level.SEVERE).withCause(e).log("/pay failed: fromUUID=" + fromPlayer.getUuid() + ", fromName=" + fromPlayer.getUsername() + ", targetName=" + targetName);
                return CompletableFuture.completedFuture(null);
            }
        } else {
            targetName = targetPlayer.getUsername();
            targetUUID = targetPlayer.getUuid();
        }

        if (targetUUID.equals(fromPlayer.getUuid())) {
            sender.sendMessage(ChatColor.toMessage(Messages.pay_self));
            return CompletableFuture.completedFuture(null);
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

        try {
            if (!storage.transfer(fromPlayer.getUuid(), fromPlayer.getUsername(), targetUUID, targetName, amount)) {
                sender.sendMessage(ChatColor.toMessage(Messages.pay_not_enough.replace("{balance}", economy.format(economy.getBalance(fromPlayer.getUuid()), false))));
                return CompletableFuture.completedFuture(null);
            }

            sender.sendMessage(ChatColor.toMessage(Messages.pay_sender.replace("{player}", targetName).replace("{amount}", economy.format(amount, false))));

            if (!isOffline) {
                targetPlayer.sendMessage(ChatColor.toMessage(Messages.pay_target.replace("{player}", fromPlayer.getUsername()).replace("{amount}", economy.format(amount, false))));
            }
        } catch (Exception e) {
            sender.sendMessage(ChatColor.toMessage(Messages.internal_error));
            logger.at(Level.SEVERE).withCause(e).log("/pay failed: fromUUID=" + fromPlayer.getUuid() + ", fromName=" + fromPlayer.getUsername() + ", targetUUID=" + targetUUID + ", targetName=" + targetName + ", amount=" + amount);
        }

        return CompletableFuture.completedFuture(null);
    }
}