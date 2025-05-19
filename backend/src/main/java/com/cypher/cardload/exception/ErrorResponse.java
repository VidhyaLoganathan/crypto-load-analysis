package com.cypher.cardload.exception;
/**
 * Error response DTO for REST API error responses.
 * This class is used to structure the error details returned to clients.
 */
public class ErrorResponse {
    private final String timestamp;
    private final int status;
    private final String error;
    private final String message;
    private final String path;

    /**
     * Creates a new error response with the provided details.
     *
     * @param timestamp the timestamp when the error occurred
     * @param status the HTTP status code
     * @param error the error type
     * @param message the error message
     * @param path the request path that triggered the error
     */
    public ErrorResponse(String timestamp, int status, String error, String message, String path) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

    /**
     * Gets the timestamp when the error occurred.
     *
     * @return the timestamp
     */
    public String getTimestamp() {
        return timestamp;
    }

    /**
     * Gets the HTTP status code.
     *
     * @return the status code
     */
    public int getStatus() {
        return status;
    }

    /**
     * Gets the error type.
     *
     * @return the error type
     */
    public String getError() {
        return error;
    }

    /**
     * Gets the error message.
     *
     * @return the error message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Gets the request path that triggered the error.
     *
     * @return the request path
     */
    public String getPath() {
        return path;
    }

    /**
     * Builder for creating ErrorResponse instances.
     */
    public static class Builder {
        private String timestamp;
        private int status;
        private String error;
        private String message;
        private String path;

        /**
         * Sets the timestamp.
         *
         * @param timestamp the timestamp
         * @return this builder
         */
        public Builder timestamp(String timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        /**
         * Sets the status code.
         *
         * @param status the status code
         * @return this builder
         */
        public Builder status(int status) {
            this.status = status;
            return this;
        }

        /**
         * Sets the error type.
         *
         * @param error the error type
         * @return this builder
         */
        public Builder error(String error) {
            this.error = error;
            return this;
        }

        /**
         * Sets the error message.
         *
         * @param message the error message
         * @return this builder
         */
        public Builder message(String message) {
            this.message = message;
            return this;
        }

        /**
         * Sets the request path.
         *
         * @param path the request path
         * @return this builder
         */
        public Builder path(String path) {
            this.path = path;
            return this;
        }

        /**
         * Builds a new ErrorResponse with the configured values.
         *
         * @return a new ErrorResponse instance
         */
        public ErrorResponse build() {
            return new ErrorResponse(timestamp, status, error, message, path);
        }
    }

    /**
     * Creates a new builder for ErrorResponse.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }
}
