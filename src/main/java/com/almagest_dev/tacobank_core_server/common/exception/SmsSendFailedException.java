package com.almagest_dev.tacobank_core_server.common.exception;

public class SmsSendFailedException extends RuntimeException {
    public SmsSendFailedException(String message) {
        super(message);
    }
    public SmsSendFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
