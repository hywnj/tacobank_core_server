package com.almagest_dev.tacobank_core_server.common.exception;

public class NaverApiException extends RuntimeException {
    public NaverApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
