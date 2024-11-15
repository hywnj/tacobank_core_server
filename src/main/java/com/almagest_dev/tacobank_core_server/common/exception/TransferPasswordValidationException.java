package com.almagest_dev.tacobank_core_server.common.exception;

import org.springframework.http.HttpStatus;

public class TransferPasswordValidationException extends RuntimeException {
    private final HttpStatus httpStatus;

    public TransferPasswordValidationException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
