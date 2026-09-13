package com.pothole.exception;

public class AiServiceTimeoutException extends AiServiceException {

    public AiServiceTimeoutException(String message, Throwable cause) {
        super("AI_SERVICE_TIMEOUT", message, cause);
    }
}
