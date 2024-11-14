package com.almagest_dev.tacobank_core_server.common.exception;
public class InvalidVerificationException extends RuntimeException {
    public InvalidVerificationException() {
        super("인증이 실패했습니다.");
    }

    public InvalidVerificationException(String message) {
        super(message);
    }
}
