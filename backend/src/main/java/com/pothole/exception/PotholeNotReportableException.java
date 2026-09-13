package com.pothole.exception;

public class PotholeNotReportableException extends RuntimeException {

    private final String errorCode;

    public PotholeNotReportableException(String message) {
        super(message);
        this.errorCode = "POTHOLE_NOT_REPORTABLE";
    }

    public PotholeNotReportableException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
