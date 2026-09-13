package com.pothole.exception;

public class StorageUnavailableException extends StorageException {

    public StorageUnavailableException(String message) {
        super(message, "STORAGE_UNAVAILABLE");
    }

    public StorageUnavailableException(String message, Throwable cause) {
        super(message, "STORAGE_UNAVAILABLE", cause);
    }
}
