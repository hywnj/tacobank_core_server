package com.almagest_dev.tacobank_core_server.common.utils;

import com.almagest_dev.tacobank_core_server.common.exception.RedisSessionException;
import com.almagest_dev.tacobank_core_server.infrastructure.encryption.EncryptionUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionUtil {
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
    public <T> void storeSessionData(String redisKey, T data, long duration, TimeUnit unit, boolean encryptFlag) {
        try {
            String jsonData = objectMapper.writeValueAsString(data);
            String valueToStore = encryptFlag ? encryptionUtil.encrypt(jsonData) : jsonData;

            redisTemplate.opsForValue().set(redisKey, valueToStore, duration, unit);
        } catch (Exception e) {
            throw new RedisSessionException("Redis 저장 중 오류", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Redis 조회 - 암호화 된 객체 데이터
     */
    public <T> T getDecryptedSessionData(String redisKey, Class<T> clazz) {
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
     * Redis 조회 - 평문 Value
     */
    public String getRawSessionData(String redisKey) {
        if (StringUtils.isBlank(redisKey)) {
            throw new RedisSessionException("유효하지 않은 요청 입니다.", HttpStatus.BAD_REQUEST);
        }

        String value = redisTemplate.opsForValue().get(redisKey);
        if (value == null) {
            throw new RedisSessionException("세션이 존재하지 않습니다.", HttpStatus.BAD_REQUEST);
        }

        return value;
    }

    /**
     * Redis 키 존재 여부 확인 후 값 반환
     * @return 키가 존재하면 값 반환, 존재하지 않으면 null 반환
     */
    public String getValueIfExists(String redisKey) {
        if (StringUtils.isBlank(redisKey)) {
            throw new RedisSessionException("유효하지 않은 요청입니다.", HttpStatus.BAD_REQUEST);
        }

        try {
            Boolean exists = redisTemplate.hasKey(redisKey);
            if (Boolean.TRUE.equals(exists)) {
                return redisTemplate.opsForValue().get(redisKey);
            } else {
                return null;
            }
        } catch (Exception e) {
            log.warn("SessionUtil::getValueIfExists Redis 키 확인 및 값 조회 중 예외 발생 - Key: {}, Error: {}", redisKey, e.getMessage());
            throw new RedisSessionException("Redis 키 확인 또는 값 조회 중 오류 발생", HttpStatus.INTERNAL_SERVER_ERROR);
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

    /**
     * Redis 키 삭제
     * @param sessionId 세션 ID
     */
    public void cleanupRedisKeys(String className, String sessionId, String... prefixes) {
        // Redis 키 삭제 로직
        for (String prefix : prefixes) {
            String key = prefix + sessionId; // 키 생성
            try {
                boolean deleted = Boolean.TRUE.equals(redisTemplate.delete(key));
                log.info("{} - [{}] Redis 키 삭제 - Key: {}, 성공 여부: {}", className, sessionId, key, deleted);
            } catch (Exception redisEx) {
                log.warn("{} - [{}] Redis 키 삭제 중 예외 발생 - Key: {}, Error: {}", className, sessionId, key, redisEx.getMessage());
            }
        }
    }

    /**
     * Redis 키의 값을 증가시키고 만료 시간을 설정
     *
     * @param redisKey Redis 키
     * @param incrementValue 증가할 값 (기본값 1L)
     * @param ttl 만료 시간 (null인 경우 기존 TTL 유지)
     * @param timeUnit TTL의 단위 (ttl이 null이면 무시됨)
     * @return 증가된 값
     */
    public Long incrementAndSetExpire(String redisKey, long incrementValue, Long ttl, TimeUnit timeUnit) {
        try {
            // 값 증가
            Long newValue = redisTemplate.opsForValue().increment(redisKey, incrementValue);

            // TTL 설정
            if (ttl != null && timeUnit != null) {
                redisTemplate.expire(redisKey, ttl, timeUnit);
            }

            return newValue;
        } catch (Exception e) {
            throw new RedisSessionException("Redis 증분 또는 TTL 설정 중 오류 발생", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
