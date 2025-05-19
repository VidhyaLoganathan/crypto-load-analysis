package com.cypher.cardload.exception;

/**
 * Exception thrown when there's an issue with wallet analysis.
 * This exception is used for errors specifically related to the wallet analysis process,
 * such as invalid wallet format, analysis calculation errors, or data processing issues.
 */
public class WalletAnalysisException extends RuntimeException {

    /**
     * Constructs a new wallet analysis exception with the specified detail message.
     *
     * @param message the detail message
     */
    public WalletAnalysisException(String message) {
        super(message);
    }

    /**
     * Constructs a new wallet analysis exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public WalletAnalysisException(String message, Throwable cause) {
        super(message, cause);
    }
}
