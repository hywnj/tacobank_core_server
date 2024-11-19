package com.almagest_dev.tacobank_core_server.common.exception;

import org.springframework.http.HttpStatus;

public class RedisSessionException extends RuntimeException {
    private final HttpStatus status;

    public RedisSessionException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
