package com.almagest_dev.tacobank_core_server.application.service;

import com.almagest_dev.tacobank_core_server.common.dto.CoreResponseDto;
import com.almagest_dev.tacobank_core_server.common.exception.TransferException;
import com.almagest_dev.tacobank_core_server.common.exception.TransferPasswordValidationException;
import com.almagest_dev.tacobank_core_server.common.utils.RedisSessionUtil;
import com.almagest_dev.tacobank_core_server.domain.account.model.Account;
import com.almagest_dev.tacobank_core_server.domain.account.repository.AccountRepository;
import com.almagest_dev.tacobank_core_server.domain.member.model.Member;
import com.almagest_dev.tacobank_core_server.domain.member.repository.MemberRepository;
import com.almagest_dev.tacobank_core_server.domain.settlememt.model.Settlement;
import com.almagest_dev.tacobank_core_server.domain.settlememt.repository.SettlementDetailsRepository;
import com.almagest_dev.tacobank_core_server.domain.settlememt.repository.SettlementRepository;
import com.almagest_dev.tacobank_core_server.domain.transfer.model.Transfer;
import com.almagest_dev.tacobank_core_server.domain.transfer.repository.TransferRepository;
import com.almagest_dev.tacobank_core_server.infrastructure.external.testbed.client.TestbedApiClient;
import com.almagest_dev.tacobank_core_server.infrastructure.external.testbed.dto.*;
import com.almagest_dev.tacobank_core_server.presentation.dto.transfer.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;



@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {
    private final TransferRepository transferRepository;
    private final MemberRepository memberRepository;
    private final AccountRepository accountRepository;
    private final SettlementRepository settlementRepository;
    private final SettlementDetailsRepository settlementDetailsRepository;

    private final PasswordEncoder passwordEncoder;
    private final TestbedApiClient testbedApiClient;
    private final RedisSessionUtil redisSessionUtil;

    private final RedisTemplate<String, String> redisTemplate;

    private final String TRANSFER_SESSION_PREFIX = "transfer:session:";
    private static final String PIN_FAILURE_PREFIX = "transfer:pw:failures:";

    /**
     * 수취인 조회
     */
    public ReceiverInquiryResponseDto inquireReceiverAccount(ReceiverInquiryRequestDto requestDto) {
        // Session ID 할당
        String sessionId = redisSessionUtil.generateSessionId(requestDto.getWithdrawalMemberId(), requestDto.getIdempotencyKey());
        log.info("TransferService - [{}] inquireReceiverAccount START", sessionId);

        // Member(송금 보내는 사람) 조회
        Member withdrawalMember = memberRepository.findByIdAndDeleted(requestDto.getWithdrawalMemberId(), "N")
                .orElseThrow(() -> new TransferException("TERMINATED", "존재하지 않는 회원입니다.", HttpStatus.BAD_REQUEST));
        // Account(송금 보내는 계좌) 조회
        Account withdrawalAccount = accountRepository.findByIdAndVerified(requestDto.getWithdrawalAccountId(), "Y")
                .orElseThrow(() -> new TransferException("TERMINATED", "인증되지 않은 계좌입니다.", HttpStatus.BAD_REQUEST));

        // 수취인 조회를 위한 Member 데이터 세팅
        ReceiverInquiryApiRequestDto receiverInquiryApiRequest = new ReceiverInquiryApiRequestDto(
                withdrawalMember.getUserFinanceId(),
                withdrawalAccount.getFintechUseNum(),
                withdrawalMember.getName(),
                requestDto.getReceiverBankCode(),
                requestDto.getReceiverAccountNum(),
                withdrawalMember.getName(), // 출금 회원명
                "0"
        );
        // Testbed 수취인 조회 API 요청
        log.info("TransferService - [{}] inquireReceiverAccount CALL 수취인 조회 API", sessionId);
        ReceiverInquiryApiResponseDto receiverInquiryApiResponse = testbedApiClient.requestApi(
                receiverInquiryApiRequest,
                "/openbank/recipient",
                ReceiverInquiryApiResponseDto.class
        );
        log.info("TransferService - [{}] inquireReceiverAccount 수취인 조회 Response: {} ", sessionId, receiverInquiryApiResponse);
        // 수취인 조회 실패
        if (receiverInquiryApiResponse.getApiTranId() == null || !receiverInquiryApiResponse.getRspCode().equals("A0000") || receiverInquiryApiResponse.getRecvAccountFintechUseNum() == null) {
            throw new TransferException("FAILURE", "확인되지 않는 계좌입니다. 다시 입력해주세요.", HttpStatus.BAD_REQUEST);
        }

        BalanceInquiryApiRequestDto balanceInquiryApiRequest = new BalanceInquiryApiRequestDto(
                withdrawalMember.getUserFinanceId(),
                withdrawalAccount.getFintechUseNum(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
        );
        log.info("TransferService - [{}] inquireReceiverAccount CALL 잔액 조회 API", sessionId);
        BalanceInquiryApiResponseDto balanceInquiryApiResponse = testbedApiClient.requestApi(
                balanceInquiryApiRequest,
                "/openbank/account",
                BalanceInquiryApiResponseDto.class
        );
        log.info("TransferService - [{}] inquireReceiverAccount 잔액 조회 Response: {} ", sessionId, balanceInquiryApiResponse);
        if (balanceInquiryApiResponse.getApiTranId() == null || !balanceInquiryApiResponse.getRspCode().equals("A0000") || balanceInquiryApiResponse.getBalanceAmt() == null) {
            throw new TransferException("TERMINATED", "계좌 잔액 조회에 실패했습니다. - " + balanceInquiryApiResponse.getRspMessage(), HttpStatus.BAD_REQUEST);
        }

        // 수취인 조회 성공시 Redis Set (TTL: 20분)
        TransferSessionData data = new TransferSessionData(
                requestDto.getIdempotencyKey(),             // 중복 방지 키(클라이언트에서 생성)
                requestDto.getWithdrawalMemberId(),         // 출금 사용자 ID
                withdrawalMember.getUserFinanceId(),        // 출금 사용자 금융 식별번호
                withdrawalAccount.getFintechUseNum(),       // 출금 계좌 핀테크 이용번호
                new WithdrawalDetails(
                        requestDto.getWithdrawalAccountId(),        // 출금 계좌 아이디
                        withdrawalAccount.getAccountNum(),       // 출금 계좌 번호
                        withdrawalAccount.getAccountHolderName(),   // 출금 예금주
                        withdrawalAccount.getBankCode()             // 출금 은행 코드
                ),
                receiverInquiryApiResponse.getRecvAccountFintechUseNum(),  // 입금(수취) 계좌 핀테크 이용번호
                new ReceiverDetails(
                        requestDto.getReceiverAccountNum(),                     // 입금(수취) 계좌 번호
                        receiverInquiryApiResponse.getAccountHolderName(),      // 입금(수취) 예금주(수취인)
                        requestDto.getReceiverBankCode()                        // 입금(수취) 은행 코드
                ),
                0,
                false
        );
        redisSessionUtil.storeSessionData(TRANSFER_SESSION_PREFIX + sessionId, data, 20, TimeUnit.MINUTES, true);
        log.info("TransferService - [{}] inquireReceiverAccount Redis Set : {} ", sessionId, receiverInquiryApiResponse);

        // 클라이언트에 응답 반환
        ReceiverInquiryResponseDto response = new ReceiverInquiryResponseDto(
                requestDto.getIdempotencyKey(),
                receiverInquiryApiResponse.getAccountHolderName(),
                withdrawalAccount.getId(),
                withdrawalAccount.getAccountNum(),
                Integer.parseInt(balanceInquiryApiResponse.getBalanceAmt())
        );
        log.info("TransferService - [{}] inquireReceiverAccount 수취인 조회 응답 : {} ", sessionId, response);
        log.info("TransferService - [{}] inquireReceiverAccount END", sessionId);
        return response;
    }

    /**
     * 송금 Validation Check
     *   - 송금액, 중복 송금 요청 확인 등
     */
    private void validateTransferData(String sessionId, TransferRequestDto requestDto, TransferSessionData sessionData) {
        log.info("TransferService - [{}] validateTransferData START", sessionId);

        String status = "TERMINATED";
        String message = "잘못된 송금 요청입니다.";
        HttpStatus httpStatus = HttpStatus.BAD_REQUEST;

        // 송금 요청 중복 체크
        if (transferRepository.existsByIdempotencyKeyAndStatusIn(requestDto.getIdempotencyKey(), List.of("S", "R"))) {
            message = "중복된 송금 요청입니다.";
            httpStatus = HttpStatus.CONFLICT;
            throw new TransferException(status, message, httpStatus);
        }

        // 송금 요청 파라미터 확인
        WithdrawalDetails withdrawalDetails = requestDto.getWithdrawalDetails();
        ReceiverDetails receiverDetails = requestDto.getReceiverDetails();
        if (withdrawalDetails.getAccountId() != sessionData.getWithdrawalDetails().getAccountId()
                || !withdrawalDetails.getAccountNum().equals(sessionData.getWithdrawalDetails().getAccountNum())
                || !withdrawalDetails.getAccountHolder().equals(sessionData.getWithdrawalDetails().getAccountHolder())
                || !withdrawalDetails.getBankCode().equals(sessionData.getWithdrawalDetails().getBankCode())
                || !receiverDetails.getAccountNum().equals(sessionData.getReceiverDetails().getAccountNum())
                || !receiverDetails.getAccountHolder().equals(sessionData.getReceiverDetails().getAccountHolder())
                || !receiverDetails.getBankCode().equals(sessionData.getReceiverDetails().getBankCode())) {
            log.warn("TransferService- [{}] validateTransferData 요청과 세션 데이터 다름 - request: {}, session: {}", sessionId, requestDto, sessionData);
            throw new TransferException(status, message, httpStatus);
        }
        // 송금액 확인
        if (requestDto.getAmount() <= 0) {
            status = "FAILURE"; // 송금액 에러 발생시, 다시 입력할 수 있게
            message = "송금액은 0원 이상이어야 합니다.";
            throw new TransferException(status, message, httpStatus);
        }
        // 정산 ID 체크
        if (requestDto.getSettlementId() != null && requestDto.getSettlementId() > 0) {
            Settlement settlement = settlementRepository.findById(requestDto.getSettlementId()).orElse(null);
            if (settlement == null) {
                log.warn("TransferService - [{}] validateTransferData 정산 정보가 없습니다. - settlement ID: {}", sessionId, requestDto.getSettlementId());
                throw new TransferException(status, message, httpStatus);
            }
        }
        log.info("TransferService - [{}] validateTransferData END", sessionId);
    }

    /**
     * 출금(이체)시 비밀번호 검증
     */
    public void verifyPassword(TransferRequestDto requestDto) {
        if (StringUtils.isBlank(requestDto.getTransferPin())) {
            throw new TransferException("FAILURE", "비밀번호를 입력해주세요.", HttpStatus.BAD_REQUEST);
        }

        String sessionId = redisSessionUtil.generateSessionId(requestDto.getMemberId(), requestDto.getIdempotencyKey());
        String transferSessionRedisKey = TRANSFER_SESSION_PREFIX + sessionId;
        String pinFailureRedisKey = PIN_FAILURE_PREFIX + sessionId;

        log.info("TransferService - [{}] verifyPassword START", sessionId);

        // Redis 조회 - 송금 요청건
        TransferSessionData sessionData = redisSessionUtil.getSessionData(transferSessionRedisKey, TransferSessionData.class, true);
        if (sessionData == null) {
            throw new TransferException("TERMINATED", "송금 요청건이 존재하지 않습니다.", HttpStatus.BAD_REQUEST);
        }
        // 송금 요청 데이터 Validation 확인
        try {
            validateTransferData(sessionId, requestDto, sessionData);
        } catch (TransferException ex) {
            if (ex.getStatus().equals("TERMINATED")) { // 완전 종료인 경우에만 삭제
                redisSessionUtil.cleanupRedisKeys("Transfer", sessionId, TRANSFER_SESSION_PREFIX, PIN_FAILURE_PREFIX);
            }
            throw ex;
        }

        // Redis 조회 - 실패 내역
        String failCntStr = redisTemplate.opsForValue().get(pinFailureRedisKey);
        // 실패 내역이 없다면 실패 횟수를 0으로 초기화
        Long failCnt = (failCntStr == null) ? 0L : Long.parseLong(failCntStr);

        // Member(송금 보내는 사람) 조회: 출금 비밀번호 조회
        Member withdrawalMember = memberRepository.findByIdAndDeleted(requestDto.getMemberId(), "N")
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 비밀번호 검증
        if (withdrawalMember.getTransferPin() == null) {
            throw new TransferException("TERMINATED", "출금 비밀번호 설정이 안되어있습니다. 비밀번호 설정 후 다시 송금해주세요.", HttpStatus.BAD_REQUEST);
        }
        boolean isValid = passwordEncoder.matches(requestDto.getTransferPin(), withdrawalMember.getTransferPin());
        if (!isValid) {
            // 실패 횟수 증가
            failCnt = redisTemplate.opsForValue().increment(pinFailureRedisKey);
            log.info("TransferService - [{}] verifyPassword 비밀번호 불일치 {}번째", sessionId, failCnt);

            // Redis에 키가 처음 생성된 경우, TTL 설정
            if (failCnt == 1) {
                redisTemplate.expire(pinFailureRedisKey, 20, TimeUnit.MINUTES);
            }

            // 실패 횟수 초과 시 송금 종료
            if (failCnt >= 5) {
                // 송금 종료시 관련 Redis 모두 삭제
                redisTemplate.delete(pinFailureRedisKey);
                redisTemplate.delete(transferSessionRedisKey);

                throw new TransferPasswordValidationException("TERMINATED", "비밀번호 입력 횟수가 초과했습니다. 송금을 종료합니다.", HttpStatus.FORBIDDEN);
            }

            // 실패 메시지 반환
            throw new TransferPasswordValidationException("비밀번호가 올바르지 않습니다. 남은 시도 횟수: " + (5 - failCnt), HttpStatus.BAD_REQUEST);
        }

        // 성공 시 Password Fail Redis 키 삭제
        redisTemplate.delete(pinFailureRedisKey);
        // 송금 요청 Redis 업데이트
        sessionData.changePasswordVerified(true);
        sessionData.assignAmount(requestDto.getAmount());
        redisSessionUtil.updateSessionData(TRANSFER_SESSION_PREFIX + sessionId, sessionData, 0, null, false, true);
        log.info("TransferService - [{}] verifyPassword sessionData UPDATE - {}", sessionId, sessionData);
        log.info("TransferService- [{}] verifyPassword END", sessionId);
    }

    /**
     * 송금
     */
    public CoreResponseDto<TransferResponseDto> transfer(TransferRequestDto requestDto) {
        // 출금 비밀번호 검증
        verifyPassword(requestDto);

        String sessionId = redisSessionUtil.generateSessionId(requestDto.getMemberId(), requestDto.getIdempotencyKey());
        String sessionKey = TRANSFER_SESSION_PREFIX + sessionId;

        log.info("TransferService - [{}] transfer START", sessionId);
        log.info("TransferService - [{}] transfer requestDto :{} ", sessionId, requestDto);

        // Redis에서 송금 세션 확인
        TransferSessionData sessionData = redisSessionUtil.getSessionData(sessionKey, TransferSessionData.class, true);
        if (sessionData == null) {
            throw new TransferException("TERMINATED", "유효하지 않은 송금 요청입니다.", HttpStatus.BAD_REQUEST);
        }
        if (!sessionData.isPasswordVerified()) {
            throw new TransferException("FAILURE", "비밀번호 검증이 완료되지 않았습니다.", HttpStatus.BAD_REQUEST);
        }
        log.info("TransferService - [{}] transfer sessionData: {} ", sessionId, sessionData);
        // 송금액 위변조 체크
        if (requestDto.getAmount() != sessionData.getAmount()) {
            log.warn("TransferService- [{}] transfer 송금액 위변조 - request amount: {}, session amount: {}", sessionId, requestDto.getAmount(), sessionData.getAmount());
            // 위변조인 경우 송금 종료 (세션 삭제)
            redisTemplate.delete(sessionKey);
            throw new TransferException("TERMINATED", "잘못된 송금 요청입니다.", HttpStatus.BAD_REQUEST);
        }
        // 입금 인자 내역, 출금 인자 내역 (Default: 보내는 사람 이름)
        String wdPrintContent = (!StringUtils.isBlank(requestDto.getWdPrintContent())) ?
                requestDto.getWdPrintContent() : sessionData.getWithdrawalDetails().getAccountHolder();
        String rcvPrintContent = (!StringUtils.isBlank(requestDto.getRcvPrintContent())) ?
                requestDto.getRcvPrintContent() : sessionData.getWithdrawalDetails().getAccountHolder();

        // 송금 요청 INSERT
        Transfer transferData = new Transfer().createTransfer(
                sessionData.getIdempotencyKey(),
                sessionData.getMemberId(), sessionData.getWithdrawalDetails().getAccountId(),
                sessionData.getWithdrawalDetails().getBankCode(), sessionData.getWithdrawalDetails().getAccountNum(), sessionData.getWithdrawalDetails().getAccountHolder(),
                wdPrintContent, rcvPrintContent,
                sessionData.getReceiverDetails().getBankCode(), sessionData.getReceiverDetails().getAccountNum(), sessionData.getReceiverDetails().getAccountHolder(),
                sessionData.getAmount()
        );
        log.info("TransferService - [{}] transfer INSERT Start...", sessionId);
        transferRepository.save(transferData);
        log.info("TransferService - [{}] transfer INSERT End...", sessionId);

        // 송금 로직 처리
        log.info("TransferService - [{}] transfer CALL processTransferTransaction", sessionId);
        CoreResponseDto<TransferResponseDto> response = processTransferTransaction(transferData, requestDto, sessionData, sessionId);

        // 응답 반환
        log.info("TransferService - [{}] transfer END", sessionId);
        return response;
    }

    public CoreResponseDto<TransferResponseDto> processTransferTransaction(Transfer transfer, TransferRequestDto requestDto,
                                                                           TransferSessionData sessionData, String sessionId) {
        log.info("TransferService - [{}] transfer processTransferTransaction START", sessionId);
        try {
            // 테스트베드 송금 요청 Body 세팅
            TransferApiRequestDto apiRequestDto = new TransferApiRequestDto(
                    sessionData.getWithdrawalUserFinanceId(),
                    "TR",
                    String.valueOf(sessionData.getAmount()),
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")),
                    sessionData.getWithdrawalFintechUseNum(),
                    transfer.getWdPrintContent(),
                    sessionData.getWithdrawalDetails().getAccountHolder(),
                    sessionData.getWithdrawalDetails().getBankCode(),
                    sessionData.getWithdrawalDetails().getAccountNum(),
                    sessionData.getReceiverFintechUseNum(),
                    sessionData.getReceiverDetails().getAccountHolder(),
                    sessionData.getReceiverDetails().getBankCode(),
                    sessionData.getReceiverDetails().getAccountNum(),
                    transfer.getRcvPrintContent()
            );
            log.info("TransferService - [{}] transfer processTransferTransaction Testbed API Request : {}", sessionId, apiRequestDto);
            // 테스트베드 송금 요청 수행
            TransferApiResponseDto apiResponse = testbedApiClient.requestApi(
                    apiRequestDto,
                    "/openbank/transfer",
                    TransferApiResponseDto.class
            );
            log.info("TransferService - [{}] transfer processTransferTransaction Testbed API Response : {}", sessionId, apiResponse);

            // @TODO testbed 응답코드 분류 받기 | 실패인 경우에도 apiTranDtm은 내려주는지 | 상세 내용이 메시지에 출력되는지 여부 & 어떤 종류가 있는지
            // 테스트베드 응답 코드 처리 & 응답 반환
            CoreResponseDto<TransferResponseDto> response;
            String apiTranId = apiResponse.getApiTranId();
            if ("A0000".equals(apiResponse.getRspCode())) {
                // 성공
                log.info("TransferService - [{}] transfer processTransferTransaction Success", sessionId);
                updateTransferStatus(transfer, apiTranId, "S", apiResponse.getRspCode(), apiResponse.getRspMessage(), apiResponse.getApiTranDtm());

                // TransferResponseDto 생성
                TransferResponseDto successResponse = TransferResponseDto.create(
                        transfer.getIdempotencyKey(),
                        apiResponse.getApiTranDtm(),
                        transfer.getMemberId(),
                        transfer.getAccountId(),
                        transfer.getWithdrawalAccountNum(),
                        transfer.getWithdrawalAccountHolder(),
                        transfer.getWithdrawalBankCode(),
                        transfer.getReceiverAccountNum(),
                        transfer.getReceiverAccountHolder(),
                        transfer.getReceiverBankCode(),
                        transfer.getAmount()
                );

                response = new CoreResponseDto<>("success", "송금이 완료되었습니다.", successResponse);

            } else {
                // 실패
                log.info("TransferService - [{}] transfer processTransferTransaction Fail : {}", sessionId, apiResponse.getRspMessage());
                updateTransferStatus(transfer, apiTranId, "F", apiResponse.getRspCode(), apiResponse.getRspMessage(), apiResponse.getApiTranDtm());

                response = new CoreResponseDto<>("fail", "송금에 실패하였습니다. (" + apiResponse.getRspMessage() + ")", null);
            }

            log.info("TransferService - [{}] transfer processTransferTransaction response - {}", sessionId, response);
            log.info("TransferService - [{}] transfer processTransferTransaction END", sessionId);
            return response;

        } catch (Exception ex) {
            log.error("TransferService - [{}] transfer processTransferTransaction Exception : {}", sessionId, ex.getMessage());
            updateTransferStatus(transfer, "", "F", "ERR001", "송금 처리 중 오류 발생", null);

            ex.printStackTrace();
            throw ex; // 예외 재발생
        } finally {
            // Redis 세션 삭제
            redisSessionUtil.cleanupRedisKeys("Transfer", sessionId, TRANSFER_SESSION_PREFIX, PIN_FAILURE_PREFIX);
        }
    }

    /**
     * Transfer UPDATE
     *  - Testbed API 요청/응답 후 DB UPDATE
     */
    public void updateTransferStatus(Transfer transfer, String apiTranId, String status, String responseCode, String responseMessage, String apiTranDtm) {
        transfer.updateTransfer(
                apiTranId,
                status,
                responseCode,
                responseMessage,
                apiTranDtm
        );
        transferRepository.save(transfer);
    }

}
