package com.pothole.exception;

public class ObjectNotFoundException extends StorageException {

    public ObjectNotFoundException(String message) {
        super(message, "OBJECT_NOT_FOUND");
    }

    public ObjectNotFoundException(String message, Throwable cause) {
        super(message, "OBJECT_NOT_FOUND", cause);
    }
}
