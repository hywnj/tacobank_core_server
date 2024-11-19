package com.almagest_dev.tacobank_core_server.application.service;

import com.almagest_dev.tacobank_core_server.common.dto.CoreResponseDto;
import com.almagest_dev.tacobank_core_server.common.exception.TransferException;
import com.almagest_dev.tacobank_core_server.common.exception.TransferPasswordValidationException;
import com.almagest_dev.tacobank_core_server.common.util.SessionUtils;
import com.almagest_dev.tacobank_core_server.domain.account.model.Account;
import com.almagest_dev.tacobank_core_server.domain.account.repository.AccountRepository;
import com.almagest_dev.tacobank_core_server.domain.member.model.Member;
import com.almagest_dev.tacobank_core_server.domain.member.repository.MemberRepository;
import com.almagest_dev.tacobank_core_server.domain.transfer.model.Transfer;
import com.almagest_dev.tacobank_core_server.domain.transfer.repository.TransferRepository;
import com.almagest_dev.tacobank_core_server.infrastructure.client.dto.TransactionDetailDto;
import com.almagest_dev.tacobank_core_server.infrastructure.client.dto.TransactionListRequestDto;
import com.almagest_dev.tacobank_core_server.infrastructure.client.dto.TransactionListResponseDto;
import com.almagest_dev.tacobank_core_server.infrastructure.client.testbed.TestbedApiClient;
import com.almagest_dev.tacobank_core_server.presentation.dto.*;
import com.almagest_dev.tacobank_core_server.presentation.dto.testbed.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;

import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {
    private final TransferRepository transferRepository;
    private final MemberRepository memberRepository;
    private final AccountRepository accountRepository;

    private final TestbedApiClient testbedApiClient;
    private final SessionUtils sessionUtils;

    private final RedisTemplate<String, String> redisTemplate;

    private final String TRANSFER_SESSION_PREFIX = "transfer:session:";
    private static final String PIN_FAILURE_PREFIX = "transfer:pw:failures:";

    /**
     * 수취인 조회
     */
    public ReceiverInquiryResponseDto inquireReceiverAccount(ReceiverInquiryRequestDto requestDto) {
        log.info("TransferService::inquireReceiverAccount START");
        // Member(송금 보내는 사람) 조회
        Member withdrawalMember = memberRepository.findByIdAndDeleted(requestDto.getWithdrawalMemberId(), "N")
                .orElseThrow(() -> new TransferException("존재하지 않는 회원입니다.", HttpStatus.BAD_REQUEST));
        // Account(송금 보내는 계좌) 조회
        Account withdrawalAccount = accountRepository.findByIdAndVerificated(requestDto.getWithdrawalAccountId(), "Y")
                .orElseThrow(() -> new TransferException("인증되지 않은 계좌입니다.", HttpStatus.BAD_REQUEST));

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
        log.info("TransferService::inquireReceiverAccount CALL 수취인 조회 API");
        ReceiverInquiryApiResponseDto receiverInquiryApiResponse = testbedApiClient.requestApi(
                receiverInquiryApiRequest,
                "/openbank/recipient",
                ReceiverInquiryApiResponseDto.class
        );
        log.info("TransferService::inquireReceiverAccount 수취인 조회 Response: {} ", receiverInquiryApiResponse);
        // 수취인 조회 실패
        if (receiverInquiryApiResponse.getApiTranId() == null || !receiverInquiryApiResponse.getRspCode().equals("A0000") || receiverInquiryApiResponse.getRecvAccountFintechUseNum() == null) {
            throw new TransferException("확인되지 않는 계좌입니다. 다시 입력해주세요.", HttpStatus.BAD_REQUEST);
        }

        BalanceInquiryApiRequestDto balanceInquiryApiRequest = new BalanceInquiryApiRequestDto(
                withdrawalMember.getUserFinanceId(),
                withdrawalAccount.getFintechUseNum(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
        );
        log.info("TransferService::inquireReceiverAccount CALL 잔액 조회 API");
        BalanceInquiryApiResponseDto balanceInquiryApiResponse = testbedApiClient.requestApi(
                balanceInquiryApiRequest,
                "/openbank/account",
                BalanceInquiryApiResponseDto.class
        );
        log.info("TransferService::inquireReceiverAccount 잔액 조회 Response: {} ", balanceInquiryApiResponse);
        if (balanceInquiryApiResponse.getApiTranId() == null || !balanceInquiryApiResponse.getRspCode().equals("A0000") || balanceInquiryApiResponse.getBalanceAmt() == null) {
            throw new TransferException("계좌 잔액조회에 실패했습니다. - " + balanceInquiryApiResponse.getRspMessage(), HttpStatus.BAD_REQUEST);
        }

        // 수취인 조회 성공시 Redis Set (TTL: 20분)
        String sessionId = sessionUtils.generateSessionId(withdrawalMember.getId(), requestDto.getIdempotencyKey());
        TransferSessionData data = new TransferSessionData(
                requestDto.getIdempotencyKey(),             // 중복 방지 키(클라이언트에서 생성)
                requestDto.getWithdrawalMemberId(),         // 출금 멤버 아이디
                withdrawalMember.getUserFinanceId(),        // 사용자 금융 식별번호
                requestDto.getWithdrawalAccountId(),        // 출금 계좌 아이디
                withdrawalAccount.getFintechUseNum(),       // 계좌 핀테크 이용번호
                withdrawalAccount.getAccountNumber(),       // 출금 계좌 번호
                withdrawalAccount.getAccountHolderName(),   // 출금 예금주
                withdrawalAccount.getBankCode(),            // 출금 은행 코드
                receiverInquiryApiResponse.getRecvAccountFintechUseNum(),  // 입금(수취) 계좌 핀테크 이용번호
                requestDto.getReceiverAccountNum(),         // 입금(수취) 계좌 번호
                receiverInquiryApiResponse.getAccountHolderName(),         // 입금(수취) 예금주(수취인)
                requestDto.getReceiverBankCode(),           // 입금(수취) 은행 코드
                0,
                false
        );
        sessionUtils.storeSessionData(TRANSFER_SESSION_PREFIX + sessionId, data, 20, TimeUnit.MINUTES);
        log.info("TransferService::inquireReceiverAccount Redis[{}] Set : {} ", TRANSFER_SESSION_PREFIX + sessionId, receiverInquiryApiResponse);

        // 클라이언트에 응답 반환
        ReceiverInquiryResponseDto response = new ReceiverInquiryResponseDto(
                requestDto.getIdempotencyKey(),
                receiverInquiryApiResponse.getAccountHolderName(),
                withdrawalAccount.getAccountNumber(),
                Integer.parseInt(balanceInquiryApiResponse.getBalanceAmt())
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
        String sessionId = sessionUtils.generateSessionId(requestDto.getWithdrawalMemberId(), requestDto.getIdempotencyKey());
        String transferSessionRedisKey = TRANSFER_SESSION_PREFIX + sessionId;
        TransferSessionData sessionData = sessionUtils.getSessionData(transferSessionRedisKey, TransferSessionData.class);
        if (sessionData == null) {
            throw new TransferException("송금 요청건이 존재하지 않습니다.", HttpStatus.BAD_REQUEST);
        }

        // Redis 조회 - 실패 내역
        String redisKey = PIN_FAILURE_PREFIX + sessionUtils.generateSessionId(requestDto.getWithdrawalMemberId(), requestDto.getIdempotencyKey());
        String failCntStr = redisTemplate.opsForValue().get(redisKey);
        // 실패 내역이 없다면 실패 횟수를 0으로 초기화
        Long failCnt = (failCntStr == null) ? 0L : Long.parseLong(failCntStr);

        // Member(송금 보내는 사람) 조회: 출금 비밀번호 조회
        Member withdrawalMember = memberRepository.findByIdAndDeleted(requestDto.getWithdrawalMemberId(), "N")
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 비밀번호 검증
        boolean isValid = withdrawalMember.getTransferPin().equals(requestDto.getTransferPin());
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

        // 성공 시 Password Fail Redis 키 삭제
        redisTemplate.delete(redisKey);
        // 송금 요청 Redis 업데이트
        sessionData.changePasswordVerified(true);
        sessionData.assignAmount(requestDto.getAmount());
        sessionUtils.updateSessionData(TRANSFER_SESSION_PREFIX + sessionId, sessionData);
        log.info("TransferService::verifyPassword sessionData UPDATE - {}", sessionData);
        log.info("TransferService::verifyPassword END");
    }

    /**
     * 송금
     */
    public CoreResponseDto<TransferResponseDto> transfer(TransferRequestDto requestDto) {
        log.info("TransferService::transfer START");
        log.info("TransferService - transfer requestDto :{} ", requestDto);
        // Redis에서 송금 세션 확인
        String sessionKey = TRANSFER_SESSION_PREFIX + sessionUtils.generateSessionId(requestDto.getWithdrawalMemberId(), requestDto.getIdempotencyKey());
        TransferSessionData sessionData = sessionUtils.getSessionData(sessionKey, TransferSessionData.class);
        if (sessionData == null) {
            throw new TransferException("유효하지 않은 송금 요청입니다.", HttpStatus.BAD_REQUEST);
        }
        if (!sessionData.isPasswordVerified()) {
            throw new TransferException("비밀번호 검증이 완료되지 않았습니다.", HttpStatus.BAD_REQUEST);
        }
        log.info("TransferService - [{}] transfer sessionData: {} ", sessionKey, sessionData);

        // 송금 요청 중복 체크
        if (transferRepository.existsByIdempotencyKeyAndStatusIn(requestDto.getIdempotencyKey(), List.of("S", "R"))) {
            throw new TransferException("중복된 송금 요청입니다.", HttpStatus.CONFLICT);
        }

        // 송금 요청 파라미터 확인
        if (requestDto.getWithdrawalAccountId() != sessionData.getWithdrawalAccountId()
                || !requestDto.getWithdrawalAccountNum().equals(sessionData.getWithdrawalAccountNum())
                || !requestDto.getWithdrawalAccountHolder().equals(sessionData.getWithdrawalAccountHolder())
                || !requestDto.getWithdrawalBankCode().equals(sessionData.getWithdrawalBankCode())
                || !requestDto.getReceiverAccountNum().equals(sessionData.getReceiverAccountNum())
                || !requestDto.getReceiverAccountHolder().equals(sessionData.getReceiverAccountHolder())
                || !requestDto.getReceiverBankCode().equals(sessionData.getReceiverBankCode())) {
            log.warn("TransferService::transfer 요청과 세션 데이터 다름 - request: {}, session: {}", requestDto, sessionData);
            throw new TransferException("잘못된 송금 요청입니다.", HttpStatus.BAD_REQUEST);
        }
        // 송금액 확인
        if (requestDto.getAmount() <= 0) {
            throw new TransferException("송금액은 0원 이상이어야 합니다.", HttpStatus.BAD_REQUEST);
        }
        // 송금액 위변조 체크
        if (requestDto.getAmount() != sessionData.getAmount()) {
            log.warn("TransferService::transfer 송금액 위변조 - request amount: {}, session amount: {}", requestDto.getAmount(), sessionData.getAmount());
            throw new TransferException("잘못된 송금 요청입니다.", HttpStatus.BAD_REQUEST);
        }
        // 입금 인자 내역, 출금 인자 내역 (Default: 보내는 사람 이름)
        String wdPrintContent = (!StringUtils.isBlank(requestDto.getWdPrintContent())) ?
                requestDto.getWdPrintContent() : requestDto.getWithdrawalAccountHolder();
        String rcvPrintContent = (!StringUtils.isBlank(requestDto.getRcvPrintContent())) ?
                requestDto.getRcvPrintContent() : requestDto.getWithdrawalAccountHolder();

        // 송금 요청 INSERT
        Transfer transferData = new Transfer().createTransfer(
                sessionData.getIdempotencyKey(),
                sessionData.getWithdrawalMemberId(), sessionData.getWithdrawalAccountId(),
                sessionData.getWithdrawalBankCode(), sessionData.getWithdrawalAccountNum(), sessionData.getWithdrawalAccountHolder(),
                wdPrintContent, rcvPrintContent,
                sessionData.getReceiverBankCode(), sessionData.getReceiverAccountNum(), sessionData.getReceiverAccountHolder(),
                sessionData.getAmount()
        );
        log.info("TransferService - [{}] transfer INSERT Start...", sessionKey);
        transferRepository.save(transferData);
        log.info("TransferService - [{}] transfer INSERT End...", sessionKey);

        // 송금 로직 처리
        log.info("TransferService - [{}] transfer CALL processTransferTransaction", sessionKey);
        CoreResponseDto<TransferResponseDto> response = processTransferTransaction(transferData, requestDto, sessionData, sessionKey);

        // 응답 반환
        log.info("TransferService::transfer END");
        return response;
    }

    public CoreResponseDto<TransferResponseDto> processTransferTransaction(Transfer transfer, TransferRequestDto requestDto,
                                                                           TransferSessionData sessionData, String sessionKey) {
        log.info("TransferService - [{}] transfer processTransferTransaction START", sessionKey);
        try {
            // 테스트베드 송금 요청 Body 세팅
            TransferApiRequestDto apiRequestDto = new TransferApiRequestDto(
                    sessionData.getWithdrawalUserFinanceId(),
                    "TR",
                    String.valueOf(sessionData.getAmount()),
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")),
                    sessionData.getWithdrawalFintechUseNum(),
                    transfer.getWdPrintContent(),
                    sessionData.getWithdrawalAccountHolder(),
                    sessionData.getWithdrawalBankCode(),
                    sessionData.getWithdrawalAccountNum(),
                    sessionData.getReceiverFintechUseNum(),
                    sessionData.getReceiverAccountHolder(),
                    sessionData.getReceiverBankCode(),
                    sessionData.getReceiverAccountNum(),
                    transfer.getRcvPrintContent()
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
            // 테스트베드 응답 코드 처리 & 응답 반환
            CoreResponseDto<TransferResponseDto> response;
            String apiTranId = apiResponse.getApiTranId();
            if ("A0000".equals(apiResponse.getRspCode())) {
                // 성공
                log.info("TransferService - [{}] transfer processTransferTransaction Success", sessionKey);
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
                log.info("TransferService - [{}] transfer processTransferTransaction Fail : {}", sessionKey, apiResponse.getRspMessage());
                updateTransferStatus(transfer, apiTranId, "F", apiResponse.getRspCode(), apiResponse.getRspMessage(), apiResponse.getApiTranDtm());

                response = new CoreResponseDto<>("fail", "송금에 실패하였습니다. (" + apiResponse.getRspMessage() + ")", null);
            }

            log.info("TransferService - [{}] transfer processTransferTransaction response - {}", sessionKey, response);
            log.info("TransferService - [{}] transfer processTransferTransaction END", sessionKey);
            return response;

        } catch (Exception ex) {
            log.error("TransferService - [{}] transfer processTransferTransaction Exception : {}", sessionKey, ex.getMessage());
            updateTransferStatus(transfer, "", "F", "ERR001", "송금 처리 중 오류 발생", null);

            ex.printStackTrace();
            throw ex; // 예외 재발생
        } finally {
            // Redis 세션 삭제
            redisTemplate.delete(sessionKey);
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

    /**
     * 거래 내역 조회
     */
    public List<TransactionResponseDto> getTransactionHistory(Long memberId) {
        // Step 1: Member ID를 통해 userFinanceId 조회
        String userFinanceId = getUserFinanceIdByMemberId(memberId);

        // Step 2: 거래 목록 조회 요청
        TransactionListRequestDto requestDto = new TransactionListRequestDto();
        requestDto.setFintechUseNum(userFinanceId);
        requestDto.setInquiryType("A"); // 기본 조회 타입
        requestDto.setInquiryBase("D"); // 날짜 기준
        requestDto.setFromDate("20240101"); // 예시값 (시작 날짜)
        requestDto.setToDate("20241231");   // 예시값 (끝 날짜)
        requestDto.setFromTime("000000");   // 예시값 (시작 시간)
        requestDto.setToTime("235959");     // 예시값 (끝 시간)
        requestDto.setSortOrder("D");       // 내림차순
        requestDto.setTranDtime("20241118120000"); // 예시값 (거래 시간)

        TransactionListResponseDto responseDto = testbedApiClient.requestApi(
                requestDto,
                "/fintech/api/openbank/tranlist",
                TransactionListResponseDto.class
        );

        // Step 3: 응답 데이터 처리 및 변환
        return responseDto.getResList().stream()
                .map(this::mapToTransactionResponseDto)
                .collect(Collectors.toList());
    }

    private String getUserFinanceIdByMemberId(Long memberId) {
        // Member 정보를 Repository를 통해 조회
        return memberRepository.findById(memberId)
                .map(Member::getUserFinanceId) // Member 객체의 userFinanceId 필드 추출
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
    }

    private TransactionResponseDto mapToTransactionResponseDto(TransactionDetailDto detailDto) {
        TransactionResponseDto dto = new TransactionResponseDto();

        String tranDate = detailDto.getTranDate(); // 이미 String 타입으로 반환
        String tranTime = detailDto.getTranTime(); // 이미 String 타입으로 반환

        dto.setTranNum(Long.valueOf(tranDate + tranTime)); // 거래 고유 ID 생성
        dto.setType(detailDto.getTranType());
        dto.setPrintContent(detailDto.getPrintContent());
        dto.setAmount(Double.valueOf(detailDto.getTranAmt())); // String 타입으로 처리
        dto.setAfterBalanceAmount(Double.valueOf(detailDto.getAfterBalanceAmt())); // String 타입으로 처리
        dto.setTranDateTime(tranDate + " " + tranTime); // 날짜와 시간을 결합하여 설정
        return dto;
    }
}
