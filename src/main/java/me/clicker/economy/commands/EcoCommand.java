package me.clicker.economy.commands;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

public final class EcoCommand extends AbstractCommandCollection {
    public EcoCommand() {
        super("eco", "Administrative balance controls");

        addSubCommand(new EcoGiveCommand());
        addSubCommand(new EcoTakeCommand());
        addSubCommand(new EcoSetCommand());
    }
}