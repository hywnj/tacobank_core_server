package com.almagest_dev.tacobank_core_server.infrastructure.sms;

import com.almagest_dev.tacobank_core_server.common.exception.SmsSendFailedException;
import com.almagest_dev.tacobank_core_server.common.exception.TransferPasswordValidationException;
import com.almagest_dev.tacobank_core_server.common.utils.SessionUtil;
import com.almagest_dev.tacobank_core_server.infrastructure.external.naver.dto.sms.Message;
import com.almagest_dev.tacobank_core_server.infrastructure.external.naver.client.NaverSmsApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
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
    private static final String SMS_FAILURE_PREFIX = "sms:failures:";
    private static final String SMS_SUCCESS_PREFIX = "sms:success:";
    private final NaverSmsApiClient naverSmsApiClient;
    private final SessionUtil sessionUtil;

    /**
     * 인증 번호 문자로 전송
     */
    public void sendVerificationCode(String tel) {
        log.info("SmsAuthUtil::sendVerificationCode START");
        String code = generateCode();
        String key = SMS_KEY_PREFIX + tel;
        log.info("SmsAuthUtil::sendVerificationCode key - {} code - {}", key, code);

        // Redis에 저장 (유효시간 3분)
        //  - key: "sms:verification:(memberTel)"
        //  - value: "(인증번호 6자리)"
        sessionUtil.storeSessionData(key, code, 3, TimeUnit.MINUTES, false);

        // Message 내용
        Message message = new Message();
        message.createMessage(tel, code);

        List<Message> messages = new ArrayList<>();
        messages.add(message);

        // 네이버 SMS API 호출
        log.info("SmsAuthUtil::sendSms - CALL NaverSmsApiClient::sendSms");
        try {
            naverSmsApiClient.sendSms(messages);
            log.info("SmsAuthUtil::sendVerificationCode END");

        } catch (SmsSendFailedException ex) {
            // 해당 인증 요청 삭제
            sessionUtil.cleanupRedisKeys("SmsAuthUtil", tel, SMS_KEY_PREFIX);
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
        String smsKey = SMS_KEY_PREFIX + tel;
        String pinFailureKey = SMS_FAILURE_PREFIX + tel;
        String storedCode = sessionUtil.getRawSessionData(smsKey);
        log.info("SmsAuthUtil::verifyCode smsKey - {} inputCode - {} storedCode - {}", smsKey, inputCode, storedCode);

        // Redis 조회 - 실패 내역
        String failCntStr = sessionUtil.getValueIfExists(pinFailureKey);
        // 실패 내역이 없다면 실패 횟수를 0으로 초기화
        Long failCnt = (failCntStr == null) ? 0L : Long.parseLong(failCntStr);

        // 비밀번호 검증
        storedCode = storedCode.replace("\"", "");
        if (inputCode.equals(storedCode)) {
            log.info("SmsAuthUtil::verifyCode SUCCESS");
            sessionUtil.cleanupRedisKeys("SmsAuthUtil", tel, SMS_FAILURE_PREFIX);

            // 성공 여부 세션 생성 (5분간 유지)
            // @TODO 세션 TTL 5분?
            sessionUtil.storeSessionData(SMS_SUCCESS_PREFIX + tel, "1", 5, TimeUnit.MINUTES, false);
            return true;
        } else {
            // 실패 횟수 증가 및 TTL 설정
            failCnt = sessionUtil.incrementAndSetExpire(pinFailureKey, 1L, 20L, TimeUnit.MINUTES);
            log.info("SmsAuthUtil - [{}] verifyCode 인증번호 불일치 {}번째", pinFailureKey, failCnt);

            // 실패 횟수 초과 시 인증 실패로 종료
            if (failCnt >= 5) {
                // 관련 Redis 모두 삭제
                sessionUtil.cleanupRedisKeys("SmsAuthUtil", tel, SMS_KEY_PREFIX, SMS_FAILURE_PREFIX);
                throw new SmsSendFailedException("FAILURE", "비밀번호 입력 횟수가 초과하여 인증이 실패하였습니다.", HttpStatus.FORBIDDEN);
            }

            // 실패 메시지 반환
            throw new SmsSendFailedException("FAILURE", "인증번호가 올바르지 않습니다. 남은 시도 횟수: " + (5 - failCnt), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * 인증 성공 여부 확인
     */
    public boolean isVerified(String tel) {
        String successKey = SMS_SUCCESS_PREFIX + tel;
        String status = sessionUtil.getRawSessionData(successKey);
        status = status.replace("\"", "");
        if ("1".equals(status)) {
            return true;
        }

        return false;
        // @TODO Redis에 없을 경우 DB 확인
        // return smsVerificationLogRepository.existsByTelAndVerifiedTrue(tel);
    }

    /**
     * SMS 관련 세션 모두 삭제
     */
    public void cleanupAllSmsSession(String tel) {
        sessionUtil.cleanupRedisKeys("SmsAuthUtil", tel, SMS_KEY_PREFIX, SMS_FAILURE_PREFIX, SMS_SUCCESS_PREFIX);
    }
}
