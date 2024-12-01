package com.almagest_dev.tacobank_core_server.common.exception;

public class TestbedApiException extends BaseCustomException {
    public TestbedApiException(String message) {
        super(message);
    }
    public TestbedApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
