package com.almagest_dev.tacobank_core_server.infrastructure.sms;

import com.almagest_dev.tacobank_core_server.common.exception.SmsSendFailedException;
import com.almagest_dev.tacobank_core_server.infrastructure.external.naver.dto.sms.Message;
import com.almagest_dev.tacobank_core_server.infrastructure.external.naver.client.NaverSmsApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmsAuthUtil {
    private static final String SMS_KEY_PREFIX = "sms:verification:";
    private final RedisTemplate<String, String> redisTemplate;
    private final NaverSmsApiClient naverSmsApiClient;

    /**
     * 인증 번호 문자로 전송
     */
    public void sendVerificationCode(String tel) {
        log.info("SmsAuthUtil::sendVerificationCode START");
        String code = generateCode();
        String key = SMS_KEY_PREFIX + tel;
        log.info("SmsAuthUtil::sendVerificationCode key/code - " + key + "/" + code);

        // Redis에 저장 (유효시간 3분)
        //  - key: "sms:verification:(memberTel)"
        //  - value: "(인증번호 6자리)"
        redisTemplate.opsForValue().set(key, code, 3, TimeUnit.MINUTES);

        // Message 내용
        Message message = new Message();
        message.createMessage(tel, code);

        List<Message> messages = new ArrayList<>();
        messages.add(message);

        // 네이버 SMS API 호출
        log.info("SmsAuthUtil::sendSms - CALL NaverSmsApiClient::sendSms");
        try {
            // @TODO 발신번호 등록 후 테스트 수행
            naverSmsApiClient.sendSms(messages);
            log.info("SmsAuthUtil::sendVerificationCode END");

        } catch (SmsSendFailedException ex) {
            redisTemplate.delete(key); // 해당 인증 요청 삭제
            throw ex; // 예외 전달
        }
    }

    /**
     * 인증 번호 생성
     */
    public String generateCode() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
    }

    /**
     * 인증 번호 검증
     */
    public boolean verifyCode(String tel, String inputCode) {
        String key = SMS_KEY_PREFIX + tel;
        String storedCode = redisTemplate.opsForValue().get(key);
        log.info("SmsAuthUtil::verifyCode key/inputCode/storedCode - " + key + "/" + inputCode + "/" + storedCode);

        if (inputCode.equals(storedCode)) {
            log.info("SmsAuthUtil::verifyCode SUCCESS");
            redisTemplate.delete(key); // 해당 인증 요청 삭제
            return true;
        } else {
            log.info("SmsAuthUtil::verifyCode FAIL");
            return false;
        }
    }
}
