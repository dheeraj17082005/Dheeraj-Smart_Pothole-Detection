package com.pothole.exception;

public class InvalidMediaException extends RuntimeException {

    private final String errorCode;

    public InvalidMediaException(String message) {
        super(message);
        this.errorCode = "INVALID_MEDIA";
    }

    public InvalidMediaException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
