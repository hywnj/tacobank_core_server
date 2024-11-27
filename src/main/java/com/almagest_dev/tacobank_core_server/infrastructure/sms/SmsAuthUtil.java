package com.almagest_dev.tacobank_core_server.infrastructure.sms;

import com.almagest_dev.tacobank_core_server.common.exception.SmsSendFailedException;
import com.almagest_dev.tacobank_core_server.common.utils.JsonUtil;
import com.almagest_dev.tacobank_core_server.common.utils.SessionUtil;
import com.almagest_dev.tacobank_core_server.domain.sms.model.SmsVerificationLog;
import com.almagest_dev.tacobank_core_server.domain.sms.repository.SmsVerificationLogRepository;
import com.almagest_dev.tacobank_core_server.infrastructure.external.naver.dto.sms.Message;
import com.almagest_dev.tacobank_core_server.infrastructure.external.naver.client.NaverSmsApiClient;
import com.almagest_dev.tacobank_core_server.infrastructure.external.naver.dto.sms.NaverSmsRequestDto;
import com.almagest_dev.tacobank_core_server.infrastructure.external.naver.dto.sms.NaverSmsResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmsAuthUtil {

    @Value("${naver.sms.from}")
    private String NAVER_SMS_FROM_NUM;
    private static final String SMS_KEY_PREFIX = "sms:verification:";
    private static final String SMS_FAILURE_PREFIX = "sms:failures:";

    private final NaverSmsApiClient naverSmsApiClient;
    private final SessionUtil sessionUtil;
    private final JsonUtil jsonUtil;
    private final SmsVerificationLogRepository smsVerificationLogRepository;

    /**
     * 인증 번호 문자로 전송
     */
    public void sendVerificationCode(Long memberId, String tel, String requestType) {
        log.info("SmsAuthUtil::sendVerificationCode START");
        String code = generateCode();
        String key = SMS_KEY_PREFIX + getSessionId(memberId, tel);

        log.info("SmsAuthUtil::sendVerificationCode key - {} code - {}", key, code);

        // Message 내용
        Message message = new Message();
        message.createMessage(tel, code);

        List<Message> messages = new ArrayList<>();
        messages.add(message);

        Long time = System.currentTimeMillis();
        // Set Request Body
        NaverSmsRequestDto requestBody = new NaverSmsRequestDto(
                "SMS"
                , "COMM"
                , "82"
                , NAVER_SMS_FROM_NUM
                , "기본 메시지 내용"
                , messages
        );

        // 문자 인증 요청 로그(SmsVerificationLog) INSERT
        SmsVerificationLog smsVerificationLog = SmsVerificationLog.createSmsVerificationLog(requestType, tel, code, jsonUtil.toJsonString(requestBody));
        smsVerificationLog = smsVerificationLogRepository.save(smsVerificationLog);

        // Redis에 저장 (유효시간 3분)
        storeVerificationData(key, smsVerificationLog.getId(), code, "REQUEST");

        // 네이버 SMS API 호출
        log.info("SmsAuthUtil::sendVerificationCode CALL NaverSmsApiClient::sendSms");
        try {
            NaverSmsResponseDto response = naverSmsApiClient.sendSms(requestBody, time);

            // 네이버 SMS 응답 로그(SmsVerificationLog) UPDATE
            smsVerificationLog.updateResponseSmsVerificationLog(
                    response.getRequestId() != null ? response.getRequestId() : "",
                    response.getRequestTime() != null ? LocalDateTime.parse(response.getRequestTime()) : null,
                    response.getStatusCode() != null ? response.getStatusCode() : "",
                    response.getStatusName() != null ? response.getStatusName() : "",
                    jsonUtil.toJsonString(response)
            );
            smsVerificationLogRepository.save(smsVerificationLog);

            // 응답 결과가 실패인 경우
            if ("fail".equals(response.getStatusName()) || !"202".equals(response.getStatusCode())) {
                log.warn("SmsAuthUtil::sendVerificationCode Naver Response Fail - {}", response);
                throw new SmsSendFailedException("인증번호 발송이 실패했습니다. 다시 시도해주세요.");
            }

        } catch (SmsSendFailedException ex) {
            // 해당 인증 요청 삭제
            sessionUtil.cleanupRedisKeys("SmsAuthUtil", getSessionId(memberId, tel), SMS_KEY_PREFIX);
            throw ex; // 예외 전달

        } finally {
            log.info("SmsAuthUtil::sendVerificationCode END");
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
    public boolean verifyCode(Long memberId, String tel, String inputCode) {
        String smsKey = SMS_KEY_PREFIX + getSessionId(memberId, tel);

        Map<String, String> verificationData = sessionUtil.getSessionData(smsKey, HashMap.class, false);
        if (verificationData.get("logId") == null || verificationData.get("verificationCode") == null || verificationData.get("verificationStatus") == null) {
            log.warn("SmsAuthUtil::verifyCode 세션 Data가 없음 - verificationData: {}" ,verificationData);
            throw new SmsSendFailedException("세션이 유효하지 않습니다.");
        }
        // 기존 성공 여부 확인
        if ("VERIFIED".equals(verificationData.get("verificationStatus"))) { // 기존에 성공한 경우
            return true;
        }

        // 인증 코드 확인
        String storedCode = verificationData.get("verificationCode");
        log.info("SmsAuthUtil::verifyCode smsKey - {} inputCode - {} storedCode - {}", smsKey, inputCode, storedCode);

        // 비밀번호 검증
        if (inputCode.equals(storedCode)) { // 성공
            log.info("SmsAuthUtil::verifyCode VERIFIED SUCCESS");

            // SmsVerificationLog 인증 상태 UPDATE
            updateSmsVerificationStatus("VERIFIED", tel, inputCode);

            // Redis 세션 삭제 - 실패 횟수 세션
            sessionUtil.cleanupRedisKeys("SmsAuthUtil", getSessionId(memberId, tel), SMS_FAILURE_PREFIX);

            // 인증 상태 업데이트 (성공)
            verificationData.put("verificationStatus", "VERIFIED");
            sessionUtil.updateSessionData(smsKey, verificationData, 3, TimeUnit.MINUTES, true, false);
            return true;
        } else {
            // 실패 횟수 증가 및 TTL 설정
            String pinFailureKey = SMS_FAILURE_PREFIX + getSessionId(memberId, tel);
            Long failCnt = sessionUtil.incrementAndSetExpire(pinFailureKey, 1L, 3, TimeUnit.MINUTES);
            log.info("SmsAuthUtil - [{}] verifyCode 인증번호 불일치 {}번째", pinFailureKey, failCnt);

            // 실패 횟수 초과 시 인증 실패로 종료
            if (failCnt >= 5) {
                // SmsVerificationLog 인증 상태 UPDATE
                updateSmsVerificationStatus("FAIL", tel, inputCode);

                // 관련 Redis 모두 삭제
                sessionUtil.cleanupRedisKeys("SmsAuthUtil", getSessionId(memberId, tel), SMS_KEY_PREFIX, SMS_FAILURE_PREFIX);
                throw new SmsSendFailedException("FAILURE", "비밀번호 입력 횟수가 초과하여 인증이 실패하였습니다.", HttpStatus.FORBIDDEN);
            }

            // 실패 메시지 반환
            throw new SmsSendFailedException("FAILURE", "인증번호가 올바르지 않습니다. 남은 시도 횟수: " + (5 - failCnt), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * SMS 관련 세션 모두 삭제
     */
    public void cleanupAllSmsSession(String tel) {
        sessionUtil.cleanupRedisKeys("SmsAuthUtil", tel, SMS_KEY_PREFIX, SMS_FAILURE_PREFIX);
    }

    /**
     * SmsVerificationLog 인증 상태 UPDATE
     */
    private SmsVerificationLog updateSmsVerificationStatus(String status, String tel, String verificationCode) {
        SmsVerificationLog smsVerificationLog = smsVerificationLogRepository.findByReceiverTelAndVerificationCode(tel, verificationCode).orElse(null);
        if (smsVerificationLog == null) {
            log.warn("SmsAuthUtil::verifyCode SmsVerificationLog 인증 상태 UPDATE 실패 - 조회된 Log 데이터 없음 (tel: {}, inputCod: {})", tel, verificationCode);
        } else {
            smsVerificationLog.updateVerificationStatus(status);
            smsVerificationLogRepository.save(smsVerificationLog);
        }
        return smsVerificationLog;
    }

    /**
     * 인증 정보 데이터 생성
     */
    private void storeVerificationData(String redisKey, Long logId, String verificationCode, String verificationStatus) {
        Map<String, String> data = new HashMap<>();
        data.put("logId", logId.toString());
        data.put("verificationCode", verificationCode);
        data.put("verificationStatus", verificationStatus);

        log.warn("SmsAuthUtil::data - {}", data);
        sessionUtil.storeSessionData(redisKey, data, 3, TimeUnit.MINUTES, false);
    }

    private String getSessionId(Long memberId, String tel) {
        return tel + ":" + memberId;
    }
}
