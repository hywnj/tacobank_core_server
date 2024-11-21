package com.almagest_dev.tacobank_core_server.common.exception;

import org.springframework.http.HttpStatus;

public abstract class BaseCustomException extends RuntimeException {
    private final HttpStatus httpStatus;
    private final String status;

    public BaseCustomException(String status, String message, HttpStatus httpStatus) {
        super(message);
        this.status = status;
        this.httpStatus = httpStatus;
    }

    public BaseCustomException(String status, String message, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getStatus() {
        return status;
    }
}