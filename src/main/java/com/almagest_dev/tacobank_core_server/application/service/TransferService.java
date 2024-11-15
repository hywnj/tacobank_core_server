package com.almagest_dev.tacobank_core_server.application.service;

import com.almagest_dev.tacobank_core_server.common.exception.TransferPasswordValidationException;
import com.almagest_dev.tacobank_core_server.domain.account.model.Account;
import com.almagest_dev.tacobank_core_server.domain.account.repository.AccountRepository;
import com.almagest_dev.tacobank_core_server.domain.member.model.Member;
import com.almagest_dev.tacobank_core_server.domain.member.repository.MemberRepository;
import com.almagest_dev.tacobank_core_server.domain.transfer.repository.TransferRepository;
import com.almagest_dev.tacobank_core_server.infrastructure.client.testbed.TestbedApiClient;
import com.almagest_dev.tacobank_core_server.infrastructure.encryption.EncryptionUtil;
import com.almagest_dev.tacobank_core_server.presentation.dto.*;
import com.almagest_dev.tacobank_core_server.presentation.dto.testbed.ReceiverInquiryApiRequestDto;
import com.almagest_dev.tacobank_core_server.presentation.dto.testbed.ReceiverInquiryApiResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {
    private final TestbedApiClient testbedApiClient;
    private final TransferRepository transferRepository;
    private final MemberRepository memberRepository;
    private final AccountRepository accountRepository;
    private final EncryptionUtil encryptionUtil;

    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate;

    private final String TRANSFER_SESSION_PREFIX = "transfer:session:";
    private static final String PIN_FAILURE_PREFIX = "transfer:pw:failures:";

    /**
     * 수취인 조회
     */
    public ReceiverInquiryResponseDto inquireReceiverAccount(ReceiverInquiryRequestDto requestDto) {
        log.info("TransferService::inquireReceiverAccount START");
        // Member(송금 보내는 사람) 조회
        Member depositMember = memberRepository.findByIdAndDeleted(requestDto.getDepositMemberId(), "N")
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
        // Account(송금 보내는 계좌) 조회
        Account depositAccount = accountRepository.findByIdAndVerificated(requestDto.getDepositAccountId(), "Y")
                .orElseThrow(() -> new IllegalArgumentException("인증되지 않은 계좌입니다."));

        // 수취인 조회를 위한 Member 데이터 세팅
        String printContent = depositMember.getName();
        ReceiverInquiryApiRequestDto apiRequestDto = new ReceiverInquiryApiRequestDto(
                depositMember.getUserFinanceId(),
                depositMember.getName(),
                requestDto.getReceiverBankCode(),
                requestDto.getReceiverAccountNum(),
                printContent,
                "0"
        );
        // Testbed 수취인 조회 API 요청
        log.info("TransferService::inquireReceiverAccount Call 수취 조회 API");
        ReceiverInquiryApiResponseDto apiResponse = testbedApiClient.requestApi(
                apiRequestDto,
                "/openbank/recipient",
                ReceiverInquiryApiResponseDto.class
        );
        log.info("TransferService::inquireReceiverAccount Response 수취 조회 - " + apiResponse);

        // 수취인 조회 성공시 Redis Set (TTL: 20분)
        String sessionId = generateSessionId(depositMember.getId(), requestDto.getIdempotencyKey());
        TransferSessionData data = new TransferSessionData(
                depositAccount.getAccountNumber(),
                depositAccount.getAccountHolderName(),
                depositAccount.getBankCode(),
                requestDto.getReceiverAccountNum(),
                apiResponse.getAccountHolderName(),
                requestDto.getReceiverBankCode(),
                "0"
        );
        storeSessionData(TRANSFER_SESSION_PREFIX + sessionId, data);

        // @TODO 출금 계좌 잔액 조회 API 요청

        // 클라이언트에 응답 반환
        ReceiverInquiryResponseDto response = new ReceiverInquiryResponseDto(
                requestDto.getIdempotencyKey(),
                printContent,
                apiResponse.getAccountHolderName()
        );
        log.info("TransferService::inquireReceiverAccount Set Redis & END");
        return response;
    }

    /**
     * 출금(이체)시 비밀번호 검증
     */
    public void verifyPassword(TransferPasswordRequestDto requestDto) {
        log.info("TransferService::verifyPassword START");

        // Redis 조회 - 송금 요청건
        String transferSessionRedisKey = TRANSFER_SESSION_PREFIX + generateSessionId(requestDto.getDepositMemberId(), requestDto.getIdempotencyKey());
        if (!redisTemplate.hasKey(transferSessionRedisKey)) {
            throw new TransferPasswordValidationException("송금 요청건이 존재하지 않습니다.", HttpStatus.BAD_REQUEST);
        }

        // Redis 조회 - 실패 내역
        String redisKey = PIN_FAILURE_PREFIX + generateSessionId(requestDto.getDepositMemberId(), requestDto.getIdempotencyKey());
        String failCntStr = redisTemplate.opsForValue().get(redisKey);
        // 실패 내역이 없다면 실패 횟수를 0으로 초기화
        Long failCnt = (failCntStr == null) ? 0L : Long.parseLong(failCntStr);

        // Member(송금 보내는 사람) 조회: 출금 비밀번호 조회
        Member depositMember = memberRepository.findByIdAndDeleted(requestDto.getDepositMemberId(), "N")
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 비밀번호 검증
        boolean isValid = depositMember.getTransferPin().equals(requestDto.getTransferPin());
        if (!isValid) {
            log.info("TransferService::verifyPassword 비밀번호 불일치");
            // 실패 횟수 증가 (원자적 연산)
            failCnt = redisTemplate.opsForValue().increment(redisKey);

            // Redis에 키가 처음 생성된 경우, TTL 설정
            if (failCnt == 1) {
                redisTemplate.expire(redisKey, 20, TimeUnit.MINUTES);
            }

            // 실패 횟수 초과 시 송금 종료
            if (failCnt >= 5) {
                // 송금 종료시 관련 Redis 모두 삭제
                redisTemplate.delete(redisKey);
                redisTemplate.delete(transferSessionRedisKey);

                throw new TransferPasswordValidationException("비밀번호 입력 횟수가 초과했습니다. 송금을 종료합니다.", HttpStatus.FORBIDDEN);
            }

            // 실패 메시지 반환
            throw new TransferPasswordValidationException("비밀번호가 올바르지 않습니다. 남은 시도 횟수: " + (5 - failCnt), HttpStatus.BAD_REQUEST);
        }

        // 성공 시 Redis 키 삭제
        redisTemplate.delete(redisKey);
        log.info("TransferService::verifyPassword END");
    }



    /**
     * 세션 ID 생성 메서드
     */
    public String generateSessionId(Long memberId, String uniqueKey) {
        if (memberId == null || uniqueKey == null || uniqueKey.isEmpty()) {
            throw new IllegalArgumentException("계정정보 또는 송금 요청정보가 유효하지 않습니다.");
        }
        String input = memberId + ":" + uniqueKey;
        try {
            return encryptionUtil.encrypt(input);
        } catch (Exception e) {
            throw new RuntimeException("Session ID 생성 실패", e);
        }
    }
    /**
     * Redis 관련 메서드
     */
    // 저장
    public void storeSessionData(String redisKey, TransferSessionData data) {
        try {
            String jsonData = objectMapper.writeValueAsString(data);
            redisTemplate.opsForValue().set(redisKey, encryptionUtil.encrypt(jsonData), 20, TimeUnit.MINUTES);
        } catch (Exception e) {
            throw new RuntimeException("Redis 저장 중 오류", e);
        }
    }
    // 조회
    public TransferSessionData getSessionData(String redisKey) {
        try {
            String jsonData = redisTemplate.opsForValue().get(redisKey);
            if (jsonData == null) {
                throw new IllegalArgumentException("Redis Session 데이터가 존재하지 않습니다.");
            }
            return objectMapper.readValue(jsonData, TransferSessionData.class);
        } catch (Exception e) {
            throw new RuntimeException("Redis 조회 중 오류", e);
        }
    }
    // 업데이트
    public void updateSessionData(String redisKey, Consumer<TransferSessionData> updater) {
        try {
            TransferSessionData data = getSessionData(redisKey);
            updater.accept(data);
            storeSessionData(redisKey, data);
        } catch (Exception e) {
            throw new RuntimeException("Redis 수정 중 오류", e);
        }
    }

}
