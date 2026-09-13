package com.pothole.exception;

public class StorageException extends RuntimeException {
    private final String errorCode;

    public StorageException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public StorageException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
