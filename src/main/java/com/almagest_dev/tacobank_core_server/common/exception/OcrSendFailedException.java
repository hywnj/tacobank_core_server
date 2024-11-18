package com.almagest_dev.tacobank_core_server.common.exception;

public class OcrSendFailedException extends RuntimeException {
    public OcrSendFailedException(String message) {
        super(message);
    }
    public OcrSendFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
