package me.clicker.economy;

/**
 * Represents the result of an economy transaction.
 *
 * @param amount       The amount that was requested to be modified (deposit/withdraw amount).
 * @param balance      The player's new balance after the transaction.
 * @param type         The transaction status (success or failure).
 * @param errorMessage Optional error message if {@link #type()} is {@link ResponseType#FAILURE}.
 */
public record EconomyResponse(double amount, double balance, ResponseType type, String errorMessage) {

    /**
     * Transaction result type.
     */
    public enum ResponseType {
        SUCCESS,
        FAILURE
    }

    /**
     * Creates a successful response.
     *
     * @param amount  the amount modified
     * @param balance the new balance after modification
     * @return success response
     */
    public static EconomyResponse success(double amount, double balance) {
        return new EconomyResponse(amount, balance, ResponseType.SUCCESS, null);
    }

    /**
     * Creates a failure response.
     *
     * @param amount       the amount that was attempted
     * @param balance      the balance at the time of failure
     * @param errorMessage reason for failure
     * @return failure response
     */
    public static EconomyResponse failure(double amount, double balance, String errorMessage) {
        return new EconomyResponse(amount, balance, ResponseType.FAILURE, errorMessage);
    }

    /**
     * Checks if the transaction was successful.
     *
     * @return true if SUCCESS, otherwise false
     */
    public boolean isSuccess() {
        return type == ResponseType.SUCCESS;
    }
}