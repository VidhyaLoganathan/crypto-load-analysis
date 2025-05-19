package com.cypher.cardload.exception;

/**
 * Exception thrown when there are issues with caching operations.
 * This exception is used for errors related to data caching, such as
 * cache persistence, invalidation, or retrieval problems.
 */
public class CacheException extends RuntimeException {

    /**
     * Constructs a new cache exception with the specified detail message.
     *
     * @param message the detail message
     */
    public CacheException(String message) {
        super(message);
    }

    /**
     * Constructs a new cache exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public CacheException(String message, Throwable cause) {
        super(message, cause);
    }
}
