package com.almagest_dev.tacobank_core_server.common.util;

import com.almagest_dev.tacobank_core_server.common.exception.RedisSessionException;
import com.almagest_dev.tacobank_core_server.infrastructure.encryption.EncryptionUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class SessionUtils {
    private final EncryptionUtil encryptionUtil;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 세션 ID 생성 메서드
     */
    public String generateSessionId(Long memberId, String uniqueKey) {
        if (memberId == null || uniqueKey == null || uniqueKey.isEmpty()) {
            throw new RedisSessionException("계정정보 또는 송금 요청정보가 유효하지 않습니다.", HttpStatus.BAD_REQUEST);
        }
        String input = memberId + ":" + uniqueKey;
        try {
            return encryptionUtil.encrypt(input);
        } catch (Exception e) {
            throw new RedisSessionException("Session ID 생성 실패", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Redis 저장
     */
    public <T> void storeSessionData(String redisKey, T data, long duration, TimeUnit unit) {
        try {
            String jsonData = objectMapper.writeValueAsString(data);
            redisTemplate.opsForValue().set(redisKey, encryptionUtil.encrypt(jsonData), duration, unit);
        } catch (Exception e) {
            throw new RedisSessionException("Redis 저장 중 오류", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Redis 조회
     */
    public <T> T getSessionData(String redisKey, Class<T> clazz) {
        if (StringUtils.isBlank(redisKey)) {
            throw new RedisSessionException("유효하지 않은 요청 입니다.", HttpStatus.BAD_REQUEST);
        }

        // Redis 송금 세션 조회
        String encryptedData = redisTemplate.opsForValue().get(redisKey);
        if (encryptedData == null) {
            throw new RedisSessionException("송금 요청 내역이 존재하지 않습니다.", HttpStatus.BAD_REQUEST);
        }

        try {
            String decryptedData = encryptionUtil.decrypt(encryptedData);
            return objectMapper.readValue(decryptedData, clazz);
        } catch (Exception e) {
            String message = (e.getMessage() == null) ? "Redis 조회 중 오류" : e.getMessage();
            throw new RedisSessionException(message, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Redis 업데이트 (만료시간 갱신 X)
     */
    public <T> void updateSessionData(String redisKey, T data) {
        try {
            // 현재 TTL 가져오기
            Long currentTtl = redisTemplate.getExpire(redisKey, TimeUnit.MILLISECONDS);
            if (currentTtl == null || currentTtl <= 0) {
                throw new RedisSessionException("Redis 키의 TTL을 가져올 수 없거나 키가 만료되었습니다.", HttpStatus.BAD_REQUEST);
            }

            // 데이터 직렬화 후 암호화
            String jsonData = objectMapper.writeValueAsString(data);
            String encryptedData = encryptionUtil.encrypt(jsonData);

            // 키와 데이터를 설정하되 TTL은 유지
            redisTemplate.opsForValue().set(redisKey, encryptedData, currentTtl, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new RedisSessionException("Redis 업데이트 중 오류 발생", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
