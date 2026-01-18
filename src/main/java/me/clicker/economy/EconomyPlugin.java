package me.clicker.economy;

import com.hypixel.hytale.server.core.event.events.player.PlayerSetupConnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import me.clicker.economy.commands.*;
import me.clicker.economy.config.Config;
import me.clicker.economy.config.Messages;
import me.clicker.economy.storage.SQLiteStorage;

import javax.annotation.Nonnull;
import java.sql.SQLException;
import java.util.logging.Level;

public class EconomyPlugin extends JavaPlugin {
    private static EconomyPlugin instance;
    private static Economy economy;
    private SQLiteStorage storage;

    public EconomyPlugin(@Nonnull JavaPluginInit init) {
        super(init);

        instance = this;
    }

    @Override
    protected void setup() {
        var dataDir = getDataDirectory();

        Config.load(dataDir);
        Messages.load(dataDir);

        storage = new SQLiteStorage(getDataDirectory().resolve("economy.db"));

        storage.init();

        economy = new EconomyImpl();

        getCommandRegistry().registerCommand(new EcoCommand());
        getCommandRegistry().registerCommand(new BalanceCommand());
        getCommandRegistry().registerCommand(new BaltopCommand());
        getCommandRegistry().registerCommand(new PayCommand());

        getEventRegistry().register(PlayerSetupConnectEvent.class, this::onPlayerJoin);
    }

    @Override
    protected void shutdown() {
        storage.shutdown();
    }

    public SQLiteStorage getStorage() {
        return storage;
    }

    public static Economy getEconomy() {
        return economy;
    }

    public void onPlayerJoin(PlayerSetupConnectEvent e) {
        try {
            storage.insertOrUpdateName(e.getUuid(), e.getUsername(), Config.balance_starting);
        } catch (SQLException ex) {
            getLogger().at(Level.SEVERE).withCause(ex).log("Failed to create/update balance row on join for " + e.getUsername() + " (" + e.getUuid() + ")");
        }
    }

    public static EconomyPlugin getInstance() {
        return instance;
    }
}