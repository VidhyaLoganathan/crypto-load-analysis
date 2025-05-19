package com.cypher.cardload.exception;

public class BlockchainServiceException extends RuntimeException {

    /**
     * Constructs a new blockchain service exception with the specified detail message.
     *
     * @param message the detail message
     */
    public BlockchainServiceException(String message) {
        super(message);
    }

    /**
     * Constructs a new blockchain service exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public BlockchainServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}