package com.royal.reserve.bank.account.api.exception;

/**
 * Exception thrown when a caller is not allowed to manage another owner's bank account.
 */
public class AccountAccessDeniedException extends RuntimeException {

    /**
     * Creates a new exception with the given detail message.
     *
     * @param message the detail message
     */
    public AccountAccessDeniedException(String message) {
        super(message);
    }
}
