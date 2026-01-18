package me.clicker.economy.storage;

import com.hypixel.hytale.logger.HytaleLogger;
import me.clicker.economy.EconomyPlugin;
import me.clicker.economy.exceptions.NoRowsAffectedException;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class SQLiteStorage {
    private final HytaleLogger logger;
    private final Path dbFile;
    private Connection connection;

    public record Row(UUID uuid, String name, double balance) {}
    public record TakeResult(boolean success, double balance, Reason reason) {
        public enum Reason {
            SUCCESS,
            NOT_ENOUGH,
            NOT_FOUND
        }
    }

    public SQLiteStorage(Path dbFile) {
        this.logger = EconomyPlugin.getInstance().getLogger();
        this.dbFile = dbFile;
    }

    public void init() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.toAbsolutePath());
            connection.setAutoCommit(true);
            createTablesIfNotExists();
            logger.at(Level.INFO).log("SQLite ready: " + dbFile);
        } catch (Exception e) {
            logger.at(Level.SEVERE).withCause(e).log("SQLite init failed");
            throw new RuntimeException(e);
        }
    }

    public void shutdown() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {}

            connection = null;
        }
    }

    private void createTablesIfNotExists() throws SQLException {
        try (var st = connection.createStatement()) {
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS balances (
                    uuid TEXT PRIMARY KEY NOT NULL,
                    name TEXT,
                    balance REAL NOT NULL DEFAULT 0.0,
                    last_updated INTEGER NOT NULL
                );
            """);

            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_balances_balance ON balances(balance DESC);");
        }
    }

    public Row getRow(UUID uuid) throws SQLException {
        try (var ps = connection.prepareStatement("SELECT uuid, name, balance FROM balances WHERE uuid=?")) {
            ps.setString(1, uuid.toString());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Row(UUID.fromString(rs.getString(1)), rs.getString(2), rs.getDouble(3));
                }
            }
        }

        return null;
    }

    public Row getRow(String name) throws SQLException {
        try (var ps = connection.prepareStatement("SELECT uuid, name, balance FROM balances WHERE LOWER(name)=LOWER(?)")) {
            ps.setString(1, name);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Row(UUID.fromString(rs.getString(1)), rs.getString(2), rs.getDouble(3));
                }
            }
        }

        return null;
    }

    public boolean setBalance(UUID uuid, String name, double amount) throws SQLException {
        try (var ps = connection.prepareStatement("INSERT INTO balances (uuid, name, balance, last_updated) VALUES (?, ?, ?, ?) ON CONFLICT(uuid) DO UPDATE SET name = COALESCE(excluded.name, balances.name), balance = excluded.balance, last_updated = excluded.last_updated")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.setDouble(3, amount);
            ps.setLong(4, System.currentTimeMillis());
            return ps.executeUpdate() > 0;
        }
    }

    public double give(UUID uuid, String name, double amount) throws SQLException, NoRowsAffectedException {
        try (var ps = connection.prepareStatement("INSERT INTO balances (uuid, name, balance, last_updated) VALUES (?, ?, ?, ?) ON CONFLICT(uuid) DO UPDATE SET name = COALESCE(excluded.name, balances.name), balance = balances.balance + excluded.balance, last_updated = excluded.last_updated RETURNING balance")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.setDouble(3, amount);
            ps.setLong(4, System.currentTimeMillis());

            try (var rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new NoRowsAffectedException();
                }

                return rs.getDouble(1);
            }
        }
    }

    public TakeResult takeIfEnough(UUID uuid, String name, double amount) throws SQLException {
        var auto = connection.getAutoCommit();
        connection.setAutoCommit(false);

        try {
            Double balance = null;

            try (var ps = connection.prepareStatement("UPDATE balances SET name = COALESCE(?, name), balance = balance - ?, last_updated = ? WHERE uuid = ? AND balance >= ? RETURNING balance")) {
                ps.setString(1, name);
                ps.setDouble(2, amount);
                ps.setLong(3, System.currentTimeMillis());
                ps.setString(4, uuid.toString());
                ps.setDouble(5, amount);

                try (var rs = ps.executeQuery()) {
                    if (rs.next()) {
                        balance = rs.getDouble(1);
                    }
                }
            }

            TakeResult result;

            if (balance != null) {
                result = new TakeResult(true, balance, TakeResult.Reason.SUCCESS);
            } else {
                try (var ps = connection.prepareStatement("SELECT balance FROM balances WHERE uuid=?")) {
                    ps.setString(1, uuid.toString());

                    try (var rs = ps.executeQuery()) {
                        if (rs.next()) {
                            balance = rs.getDouble(1);
                        }
                    }
                }

                if (balance != null) {
                    result = new TakeResult(false, balance, TakeResult.Reason.NOT_ENOUGH);
                } else {
                    result = new TakeResult(false, 0.0, TakeResult.Reason.NOT_FOUND);
                }
            }

            connection.commit();
            return result;
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ignore) {}

            throw e;
        } finally {
            try {
                connection.setAutoCommit(auto);
            } catch (SQLException ignore) {}
        }
    }

    public boolean transfer(UUID fromUUID, String fromName, UUID toUUID, String toName, double amount) throws SQLException {
        try {
            connection.setAutoCommit(false);

            var now = System.currentTimeMillis();

            try (var ps = connection.prepareStatement("UPDATE balances SET balance = balance - ?, name = COALESCE(?, name), last_updated = ? WHERE uuid=? AND balance >= ?")) {
                ps.setDouble(1, amount);
                ps.setString(2, fromName);
                ps.setLong(3, now);
                ps.setString(4, fromUUID.toString());
                ps.setDouble(5, amount);

                if (ps.executeUpdate() == 0) {
                    connection.rollback();
                    return false;
                }
            }

            try (var ps = connection.prepareStatement("INSERT INTO balances (uuid, name, balance, last_updated) VALUES (?, ?, ?, ?) ON CONFLICT(uuid) DO UPDATE SET balance = balance + excluded.balance, name = COALESCE(excluded.name, balances.name), last_updated=excluded.last_updated")) {
                ps.setString(1, toUUID.toString());
                ps.setString(2, toName);
                ps.setDouble(3, amount);
                ps.setLong(4, now);
                ps.executeUpdate();
            }

            connection.commit();
            return true;
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (Exception ignored) { }

            throw e;
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (Exception ignored) { }
        }
    }

    public void insertOrUpdateName(UUID uuid, String name, double startingBalance) throws SQLException {
        try (var ps = connection.prepareStatement("INSERT INTO balances (uuid, name, balance, last_updated) VALUES (?, ?, ?, ?) ON CONFLICT(uuid) DO UPDATE SET name=excluded.name, last_updated=excluded.last_updated")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.setDouble(3, startingBalance);
            ps.setLong(4, System.currentTimeMillis());
            ps.executeUpdate();
        }
    }

    public boolean exists(String name) throws SQLException {
        try (var ps = connection.prepareStatement("SELECT 1 FROM balances WHERE LOWER(name)=LOWER(?) LIMIT 1")) {
            ps.setString(1, name);

            try (var rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public List<Row> getTop(int limit, int offset) throws SQLException {
        ArrayList<Row> out = new ArrayList<>();

        try (var ps = connection.prepareStatement("SELECT uuid, COALESCE(name,''), balance FROM balances ORDER BY balance DESC LIMIT ? OFFSET ?")) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new Row(UUID.fromString(rs.getString(1)), rs.getString(2), rs.getDouble(3)));
                }
            }
        }

        return out;
    }

    public double totalBalance() throws SQLException {
        try (var ps = connection.prepareStatement("SELECT COALESCE(SUM(balance), 0.0) FROM balances")) {
            try (var rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0.0;
            }
        }
    }

    public int countRows() throws SQLException {
        try (var ps = connection.prepareStatement("SELECT COUNT(*) FROM balances")) {
            try (var rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }
}
