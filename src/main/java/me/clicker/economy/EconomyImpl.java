package me.clicker.economy;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import me.clicker.economy.config.Config;
import me.clicker.economy.storage.SQLiteStorage;

import java.util.UUID;
import java.util.logging.Level;

public class EconomyImpl implements Economy {
    private final HytaleLogger logger;
    private final SQLiteStorage storage;

    public EconomyImpl() {
        this.logger = EconomyPlugin.getInstance().getLogger();
        this.storage = EconomyPlugin.getInstance().getStorage();
    }

    @Override
    public String getCurrencySymbol() {
        return Config.currency_symbol;
    }

    @Override
    public String format(double amount, boolean compact) {
        var symbol = getCurrencySymbol();

        if (!Double.isFinite(amount)) {
            return symbol + "0";
        }

        var negative = amount < 0;
        var value = Math.abs(amount);

        var digits = Math.max(0, Config.currency_fraction_digits);

        if (!compact) {
            var full = String.format(java.util.Locale.US, "%,." + digits + "f", value);

            if (digits > 0) {
                var zeros = "." + "0".repeat(digits);
                if (full.endsWith(zeros)) {
                    full = full.substring(0, full.length() - zeros.length());
                }
            }

            return symbol + (negative ? "-" + full : full);
        }

        var suffix = "";
        double shown = value;

        if (value >= 1_000_000_000_000.0) {
            shown = value / 1_000_000_000_000.0;
            suffix = "t";
        } else if (value >= 1_000_000_000.0) {
            shown = value / 1_000_000_000.0;
            suffix = "b";
        } else if (value >= 1_000_000.0) {
            shown = value / 1_000_000.0;
            suffix = "m";
        } else if (value >= 1_000.0) {
            shown = value / 1_000.0;
            suffix = "k";
        }

        var number = String.format(java.util.Locale.US, "%." + digits + "f", shown);

        if (digits > 0) {
            var zeros = "." + "0".repeat(digits);
            if (number.endsWith(zeros)) {
                number = number.substring(0, number.length() - zeros.length());
            }
        }

        var result = number + suffix;
        return symbol + (negative ? "-" + result : result);
    }

    @Override
    public double parseFormatted(String formatted) {
        if (formatted == null) {
            throw new NumberFormatException("null");
        }

        var s = formatted.trim();

        if (s.isEmpty()) {
            throw new NumberFormatException("empty");
        }

        var negative = false;

        if (s.startsWith("-")) {
            negative = true;
            s = s.substring(1).trim();

            if (s.isEmpty()) {
                throw new NumberFormatException("just '-'");
            }
        }

        var symbol = getCurrencySymbol();

        if (symbol != null && !symbol.isBlank() && s.startsWith(symbol)) {
            s = s.substring(symbol.length()).trim();

            if (s.isEmpty()) {
                throw new NumberFormatException("missing number after symbol");
            }
        }

        var multiplier = 1.0;
        var last = Character.toLowerCase(s.charAt(s.length() - 1));

        if (last == 'k' || last == 'm' || last == 'b' || last == 't') {
            s = s.substring(0, s.length() - 1).trim();

            if (s.isEmpty()) {
                throw new NumberFormatException("missing number before suffix");
            }

            multiplier = switch (last) {
                case 'k' -> 1_000.0;
                case 'm' -> 1_000_000.0;
                case 'b' -> 1_000_000_000.0;
                case 't' -> 1_000_000_000_000.0;
                default -> 1.0;
            };
        }

        s = s.replace(",", "").trim();

        if (s.isEmpty()) {
            throw new NumberFormatException("empty after cleanup");
        }

        var value = Double.parseDouble(s) * multiplier;
        return negative ? -value : value;
    }

    @Override
    public double getBalance(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return 0.0;
        }

        try {
            var row = storage.getRow(playerName);
            return row != null ? row.balance() : 0.0;
        } catch (Exception e) {
            throw new RuntimeException("getBalance failed for username=" + playerName, e);
        }
    }

    @Override
    public double getBalance(UUID playerUUID) {
        if (playerUUID == null) {
            return 0.0;
        }

        try {
            var row = storage.getRow(playerUUID);
            return row != null ? row.balance() : 0.0;
        } catch (Exception e) {
            throw new RuntimeException("getBalance failed for uuid=" + playerUUID, e);
        }
    }

    @Override
    public double getBalance(PlayerRef playerRef) {
        if (playerRef == null) {
            return 0.0;
        }

        return getBalance(playerRef.getUuid());
    }

    @Override
    public boolean has(String playerName, double amount) {
        if (playerName == null || playerName.isBlank() || !Double.isFinite(amount)) {
            return false;
        }

        return getBalance(playerName) >= amount;
    }

    @Override
    public boolean has(UUID playerUUID, double amount) {
        if (playerUUID == null || !Double.isFinite(amount)) {
            return false;
        }

        return getBalance(playerUUID) >= amount;
    }

    @Override
    public boolean has(PlayerRef playerRef, double amount) {
        if (playerRef == null) {
            return false;
        }

        return has(playerRef.getUuid(), amount);
    }

    @Override
    public EconomyResponse withdraw(String playerName, double amount) {
        if (playerName == null || playerName.isBlank()) {
            return EconomyResponse.failure(amount, 0.0, "Player not found.");
        }

        try {
            var row = storage.getRow(playerName);
            return row == null ? EconomyResponse.failure(amount, 0.0, "Player not found.") : withdraw(row.uuid(), amount);
        } catch (Exception e) {
            this.logger.at(Level.SEVERE).withCause(e).log("Failed to withdraw for " + playerName + " (N/A), amount=" + amount);
            return EconomyResponse.failure(amount, 0.0, "Internal error.");
        }
    }

    @Override
    public EconomyResponse withdraw(UUID playerUUID, double amount) {
        if (playerUUID == null) {
            return EconomyResponse.failure(amount, 0.0, "Player not found.");
        }

        if (!Double.isFinite(amount) || amount <= 0) {
            return EconomyResponse.failure(amount, getBalance(playerUUID), "Amount must be positive.");
        }

        try {
            var result = storage.takeIfEnough(playerUUID, null, amount);

            if (!result.success()) {
                if (result.reason() == SQLiteStorage.TakeResult.Reason.NOT_ENOUGH) {
                    return EconomyResponse.failure(amount, result.balance(), "Not enough funds.");
                }

                return EconomyResponse.failure(amount, 0.0, "Player not found.");
            }

            return EconomyResponse.success(amount, result.balance());
        } catch (Exception e) {
            this.logger.at(Level.SEVERE).withCause(e).log("Failed to withdraw for N/A (" + playerUUID + "), amount=" + amount);
            return EconomyResponse.failure(amount, 0.0, "Internal error.");
        }
    }

    @Override
    public EconomyResponse withdraw(PlayerRef playerRef, double amount) {
        if (playerRef == null) {
            return EconomyResponse.failure(amount, 0.0, "Player not found.");
        }

        if (!Double.isFinite(amount) || amount <= 0) {
            return EconomyResponse.failure(amount, getBalance(playerRef.getUuid()), "Amount must be positive.");
        }

        try {
            var result = storage.takeIfEnough(playerRef.getUuid(), playerRef.getUsername(), amount);

            if (!result.success()) {
                if (result.reason() == SQLiteStorage.TakeResult.Reason.NOT_ENOUGH) {
                    return EconomyResponse.failure(amount, result.balance(), "Not enough funds.");
                }

                return EconomyResponse.failure(amount, 0.0, "Player not found.");
            }

            return EconomyResponse.success(amount, result.balance());
        } catch (Exception e) {
            this.logger.at(Level.SEVERE).withCause(e).log("Failed to withdraw for " + playerRef.getUsername() + " (" + playerRef.getUuid() + "), amount=" + amount);
            return EconomyResponse.failure(amount, 0.0, "Internal error.");
        }
    }

    @Override
    public EconomyResponse deposit(String playerName, double amount) {
        if (playerName == null || playerName.isBlank()) {
            return EconomyResponse.failure(amount, 0.0, "Player not found.");
        }

        try {
            var row = storage.getRow(playerName);
            return row == null ? EconomyResponse.failure(amount, 0.0, "Player not found.") : deposit(row.uuid(), amount);
        } catch (Exception e) {
            this.logger.at(Level.SEVERE).withCause(e).log("Failed to deposit for " + playerName + " (N/A), amount=" + amount);
            return EconomyResponse.failure(amount, 0.0, "Internal error.");
        }
    }

    @Override
    public EconomyResponse deposit(UUID playerUUID, double amount) {
        if (playerUUID == null) {
            return EconomyResponse.failure(amount, 0.0, "Player not found.");
        }

        if (!Double.isFinite(amount) || amount <= 0) {
            return EconomyResponse.failure(amount, getBalance(playerUUID), "Amount must be positive.");
        }

        try {
            return EconomyResponse.success(amount, storage.give(playerUUID, null, amount));
        } catch (Exception e) {
            this.logger.at(Level.SEVERE).withCause(e).log("Failed to deposit for N/A (" + playerUUID + "), amount=" + amount);
            return EconomyResponse.failure(amount, 0.0, "Internal error.");
        }
    }

    @Override
    public EconomyResponse deposit(PlayerRef playerRef, double amount) {
        if (playerRef == null) {
            return EconomyResponse.failure(amount, 0.0, "Player not found.");
        }

        if (!Double.isFinite(amount) || amount <= 0) {
            return EconomyResponse.failure(amount, getBalance(playerRef.getUuid()), "Amount must be positive.");
        }

        try {
            return EconomyResponse.success(amount, storage.give(playerRef.getUuid(), playerRef.getUsername(), amount));
        } catch (Exception e) {
            this.logger.at(Level.SEVERE).withCause(e).log("Failed to deposit for " + playerRef.getUsername() + " (" + playerRef.getUuid() + "), amount=" + amount);
            return EconomyResponse.failure(amount, 0.0, "Internal error.");
        }
    }
}
