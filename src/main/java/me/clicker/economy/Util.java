package me.clicker.economy;

import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

public class Util {
    public static PlayerRef getPlayerRef(CommandSender sender) {
        if (sender == null) {
            return null;
        }

        return Universe.get().getPlayer(sender.getUuid());
    }
}