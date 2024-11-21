package com.almagest_dev.tacobank_core_server.common.exception;

import org.springframework.http.HttpStatus;

public class SmsSendFailedException extends BaseCustomException {
    public SmsSendFailedException(String status, String message, HttpStatus httpStatus) {
        super(status, message, httpStatus);
    }
    public SmsSendFailedException(String status, String message, HttpStatus httpStatus, Throwable cause) {
        super(status, message, httpStatus, cause);
    }
}
