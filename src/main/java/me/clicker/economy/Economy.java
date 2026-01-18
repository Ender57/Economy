package me.clicker.economy;

import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.UUID;

/**
 * The main economy API
 *
 */
public interface Economy {
    /**
     * Gets the active Economy API instance.
     *
     * <p>This is a convenience method so other plugins/classes can do:</p>
     * <pre>{@code
     * Economy eco = Economy.get();
     * }</pre>
     *
     * @return the active {@link Economy} instance
     * @throws IllegalStateException if the economy system is not initialized yet
     */
    static Economy get() {
        return null;
    }

    /**
     * Gets the currency symbol used by the economy system.
     *
     * @return currency symbol (example: "$")
     */
    public String getCurrencySymbol();

    /**
     * Formats an amount into a human-readable currency string.
     *
     * <p>The returned string includes the currency symbol (if configured), and supports two modes:</p>
     * <ul>
     *   <li><b>compact=true</b>: uses suffixes (k/m/b/t)</li>
     *   <li><b>compact=false</b>: uses comma grouping (1,000 / 2,500,000)</li>
     * </ul>
     *
     * <p>Examples:</p>
     * <ul>
     *   <li>compact=true  -> "$1k", "$2.5m", "-$10b"</li>
     *   <li>compact=false -> "$1,000", "$2,500,000", "-$10,000,000,000"</li>
     * </ul>
     *
     * @param amount  the amount to format
     * @param compact if true, uses suffixes (k/m/b/t). if false, uses comma grouping
     * @return formatted currency string (with symbol)
     */
    public String format(double amount, boolean compact);

    /**
     * Parses a formatted currency string back into a number.
     *
     * <p>Supports optional currency symbol, optional comma grouping, and optional suffixes (k/m/b/t).</p>
     *
     * <p>Examples:</p>
     * <ul>
     *   <li>"$1k" -> 1000</li>
     *   <li>"1k" -> 1000</li>
     *   <li>"$2.5m" -> 2500000</li>
     *   <li>"1,000" -> 1000</li>
     *   <li>"-$10b" -> -10000000000</li>
     * </ul>
     *
     * @param formatted the formatted string (symbol optional, supports commas and suffixes: k/m/b/t)
     * @return parsed numeric value
     * @throws NumberFormatException if the input is invalid
     */
    public double parseFormatted(String formatted);

    /**
     * Gets a player's current balance by their name.
     *
     * @param playerName the player's name
     * @return the player's balance
     */
    public double getBalance(String playerName);

    /**
     * Gets a player's current balance by their UUID.
     *
     * @param playerUUID the player's UUID
     * @return the player's balance
     */
    public double getBalance(UUID playerUUID);

    /**
     * Gets a player's current balance by PlayerRef.
     *
     * @param playerRef the player reference
     * @return the player's balance
     */
    public double getBalance(PlayerRef playerRef);

    /**
     * Checks if a player has at least the given amount (by name).
     *
     * @param playerName the player's name
     * @param amount the amount required
     * @return true if the player has enough money, otherwise false
     */
    public boolean has(String playerName, double amount);

    /**
     * Checks if a player has at least the given amount (by UUID).
     *
     * @param playerUUID the player's UUID
     * @param amount the amount required
     * @return true if the player has enough money, otherwise false
     */
    public boolean has(UUID playerUUID, double amount);

    /**
     * Checks if a player has at least the given amount (by PlayerRef).
     *
     * @param playerRef the player reference
     * @param amount the amount required
     * @return true if the player has enough money, otherwise false
     */
    public boolean has(PlayerRef playerRef, double amount);

    /**
     * Withdraws money from a player's balance (by name).
     *
     * @param playerName the player's name
     * @param amount the amount to withdraw
     * @return an {@link EconomyResponse} describing the result
     */
    public EconomyResponse withdraw(String playerName, double amount);

    /**
     * Withdraws money from a player's balance (by UUID).
     *
     * @param playerUUID the player's UUID
     * @param amount the amount to withdraw
     * @return an {@link EconomyResponse} describing the result
     */
    public EconomyResponse withdraw(UUID playerUUID, double amount);

    /**
     * Withdraws money from a player's balance (by PlayerRef).
     *
     * @param playerRef the player reference
     * @param amount the amount to withdraw
     * @return an {@link EconomyResponse} describing the result
     */
    public EconomyResponse withdraw(PlayerRef playerRef, double amount);

    /**
     * Deposits money into a player's balance (by name).
     *
     * @param playerName the player's name
     * @param amount the amount to deposit
     * @return an {@link EconomyResponse} describing the result
     */
    public EconomyResponse deposit(String playerName, double amount);

    /**
     * Deposits money into a player's balance (by UUID).
     *
     * @param playerUUID the player's UUID
     * @param amount the amount to deposit
     * @return an {@link EconomyResponse} describing the result
     */
    public EconomyResponse deposit(UUID playerUUID, double amount);

    /**
     * Deposits money into a player's balance (by PlayerRef).
     *
     * @param playerRef the player reference
     * @param amount the amount to deposit
     * @return an {@link EconomyResponse} describing the result
     */
    public EconomyResponse deposit(PlayerRef playerRef, double amount);
}