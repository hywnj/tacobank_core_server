package com.almagest_dev.tacobank_core_server.common.constants;

public class RedisKeyConstants { // Redis Key 상수
    public static final String LOCK_PREFIX = "member:lock:";
    public static final String SMS_KEY_PREFIX = "sms:verification:";
    public static final String SMS_FAILURE_PREFIX = "sms:failures:";

    private RedisKeyConstants() {

    }
}
