package com.almagest_dev.tacobank_core_server.application.service;

import com.almagest_dev.tacobank_core_server.common.exception.TransferPasswordValidationException;
import com.almagest_dev.tacobank_core_server.domain.account.model.Account;
import com.almagest_dev.tacobank_core_server.domain.account.repository.AccountRepository;
import com.almagest_dev.tacobank_core_server.domain.member.model.Member;
import com.almagest_dev.tacobank_core_server.domain.member.repository.MemberRepository;
import com.almagest_dev.tacobank_core_server.domain.transfer.model.Transfer;
import com.almagest_dev.tacobank_core_server.domain.transfer.repository.TransferRepository;
import com.almagest_dev.tacobank_core_server.infrastructure.client.testbed.TestbedApiClient;
import com.almagest_dev.tacobank_core_server.infrastructure.encryption.EncryptionUtil;
import com.almagest_dev.tacobank_core_server.presentation.dto.*;
import com.almagest_dev.tacobank_core_server.presentation.dto.testbed.ReceiverInquiryApiRequestDto;
import com.almagest_dev.tacobank_core_server.presentation.dto.testbed.ReceiverInquiryApiResponseDto;
import com.almagest_dev.tacobank_core_server.presentation.dto.testbed.TransferApiRequestDto;
import com.almagest_dev.tacobank_core_server.presentation.dto.testbed.TransferApiResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {
    private final TransferRepository transferRepository;
    private final MemberRepository memberRepository;
    private final AccountRepository accountRepository;

    private final TestbedApiClient testbedApiClient;
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
        String printContent = (requestDto.getPrintContent() == null) ? depositMember.getName() : requestDto.getPrintContent();
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
        log.info("TransferService::inquireReceiverAccount 수취인 조회 Response : {} ", apiResponse);

        // @TODO 출금 계좌 잔액 조회 API 요청
        log.info("TransferService::inquireReceiverAccount 출금 계좌 잔액 조회 Request : ");

        // 수취인 조회 성공시 Redis Set (TTL: 20분)
        String sessionId = generateSessionId(depositMember.getId(), requestDto.getIdempotencyKey());
        TransferSessionData data = new TransferSessionData(
                requestDto.getDepositMemberId(),        // 출금 멤버 아이디
                depositMember.getUserFinanceId(),       // 사용자 금융 식별번호
                requestDto.getDepositAccountId(),       // 출금 계좌 아이디
                depositAccount.getFintechUseNum(),      // 계좌 핀테크 이용번호
                depositAccount.getAccountNumber(),      // 출금 계좌 번호
                depositAccount.getAccountHolderName(),  // 출금 예금주
                depositAccount.getBankCode(),           // 출금 은행 코드
                requestDto.getReceiverAccountNum(),     // 입금(수취) 계좌 번호
                apiResponse.getAccountHolderName(),     // 입금(수취) 예금주(수취인)
                requestDto.getReceiverBankCode(),       // 입금(수취) 은행 코드
                "0"
        );
        storeSessionData(TRANSFER_SESSION_PREFIX + sessionId, data);
        log.info("TransferService::inquireReceiverAccount Redis[{}] Set : {} ", TRANSFER_SESSION_PREFIX + sessionId, apiResponse);

        // 클라이언트에 응답 반환
        ReceiverInquiryResponseDto response = new ReceiverInquiryResponseDto(
                requestDto.getIdempotencyKey(),
                printContent,
                apiResponse.getAccountHolderName()
        );
        log.info("TransferService::inquireReceiverAccount 수취인 조회 응답 : {} ", response);
        log.info("TransferService::inquireReceiverAccount END");
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
            // 실패 횟수 증가 (원자적 연산)
            failCnt = redisTemplate.opsForValue().increment(redisKey);
            log.info("TransferService::verifyPassword 비밀번호 불일치 {}번째", failCnt);

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
     * 송금
     */
    public void transfer(TransferRequestDto requestDto) {
        log.info("TransferService::transfer START");
        // Redis에서 송금 세션 확인
        String sessionKey = TRANSFER_SESSION_PREFIX + generateSessionId(requestDto.getDepositMemberId(), requestDto.getIdempotencyKey());
        TransferSessionData sessionData = getSessionData(sessionKey);
        if (sessionData == null) {
            throw new RuntimeException("유효하지 않은 송금 요청입니다.");
        }
        log.info("TransferService - [{}] transfer sessionData :{} ", sessionKey, sessionData);

        // 송금 요청 INSERT
        Transfer transferData = new Transfer().createTransfer(
                requestDto.getIdempotencyKey(),
                requestDto.getDepositMemberId(), requestDto.getDepositAccountId(),
                requestDto.getDepositBankCode(), requestDto.getDepositAccountNum(), requestDto.getDepositAccountHolder(),
                requestDto.getPrintContent(),
                requestDto.getReceiverBankCode(), requestDto.getReceiverAccountNum(), requestDto.getReceiverAccountHolder(),
                requestDto.getAmount()
        );
        log.info("TransferService - [{}] transfer INSERT Start...", sessionKey);
        transferRepository.save(transferData);
        log.info("TransferService - [{}] transfer INSERT End...", sessionKey);

        // 송금 로직 처리
        log.info("TransferService - [{}] transfer CALL processTransferTransaction", sessionKey);
        processTransferTransaction(transferData, requestDto, sessionData, sessionKey);

        // 응답 반환


        log.info("TransferService::transfer END");
    }

    @Transactional
    public void processTransferTransaction(Transfer transfer, TransferRequestDto requestDto,
                                           TransferSessionData sessionData, String sessionKey) {
        log.info("TransferService - [{}] transfer processTransferTransaction START", sessionKey);
        try {
            // 테스트베드 송금 요청 Body 세팅
            TransferApiRequestDto apiRequestDto = new TransferApiRequestDto(
                    sessionData.getUserFinanceId(),
                    "TR",
                    sessionData.getAmount(),
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")),
                    sessionData.getDepositFintechUseNum(),
                    requestDto.getPrintContent(),
                    sessionData.getDepositAccountHolder(),
                    sessionData.getDepositBankCode(),
                    sessionData.getDepositAccountNum(),
                    sessionData.getReceiverAccountNum(),
                    sessionData.getReceiverAccountHolder(),
                    sessionData.getReceiverBankCode(),
                    sessionData.getReceiverAccountNum(),
                    requestDto.getPrintContent()
            );
            log.info("TransferService - [{}] transfer processTransferTransaction Testbed API Request : {}", sessionKey, apiRequestDto);
            // 테스트베드 송금 요청 수행
            TransferApiResponseDto apiResponse = testbedApiClient.requestApi(
                    apiRequestDto,
                    "/openbank/transfer",
                    TransferApiResponseDto.class
            );
            log.info("TransferService - [{}] transfer processTransferTransaction Testbed API Response : {}", sessionKey, apiResponse);

            // @TODO testbed 응답코드 분류 받기 | 실패인 경우에도 apiTranDtm은 내려주는지 | 상세 내용이 메시지에 출력되는지 여부 & 어떤 종류가 있는지
            // 테스트베드 응답 코드 처리
            String apiTranId = apiResponse.getApiTranId();
            if ("A0000".equals(apiResponse.getRspCode())) {
                log.info("TransferService - [{}] transfer processTransferTransaction Success", sessionKey);
                updateTransferStatus(transfer, apiTranId, "S", apiResponse.getRspCode(), apiResponse.getRspMessage(), apiResponse.getApiTranDtm());
            } else {
                log.info("TransferService - [{}] transfer processTransferTransaction Fail : {}", sessionKey, apiResponse.getRspMessage());
                updateTransferStatus(transfer, apiTranId, "F", apiResponse.getRspCode(), apiResponse.getRspMessage(), apiResponse.getApiTranDtm());
            }
            log.info("TransferService - [{}] transfer processTransferTransaction END", sessionKey);

        } catch (Exception ex) {
            log.error("TransferService - [{}] transfer processTransferTransaction Exception : {}", sessionKey, ex.getMessage());
            ex.printStackTrace();
            updateTransferStatus(transfer, "", "F", "ERR001", "송금 처리 중 오류 발생", LocalDateTime.now().toString());

            throw ex; // 예외 재발생
        }
    }
    private void updateTransferStatus(Transfer transfer, String apiTranId, String status, String responseCode, String responseMessage, String apiTranDtm) {
        transfer.updateTransfer(
                apiTranId,
                status,
                responseCode,
                responseMessage,
                apiTranDtm != null ? apiTranDtm : LocalDateTime.now().toString()
        );
        transferRepository.save(transfer);
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
            // Redis에서 암호화된 데이터 조회
            String encryptedData = redisTemplate.opsForValue().get(redisKey);
            if (encryptedData == null) {
                throw new IllegalArgumentException("세션 데이터가 존재하지 않습니다.");
            }
            // 복호화
            String decryptedData = encryptionUtil.decrypt(encryptedData);
            // JSON -> 객체 변환
            return objectMapper.readValue(decryptedData, TransferSessionData.class);
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
