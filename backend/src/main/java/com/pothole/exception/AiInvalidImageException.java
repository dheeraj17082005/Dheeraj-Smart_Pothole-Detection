package com.pothole.exception;

public class AiInvalidImageException extends AiServiceException {

    public AiInvalidImageException(String message) {
        super("INVALID_IMAGE", message);
    }
}
