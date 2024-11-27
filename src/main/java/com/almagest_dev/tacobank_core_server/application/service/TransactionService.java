package com.almagest_dev.tacobank_core_server.application.service;


import com.almagest_dev.tacobank_core_server.domain.member.model.Member;
import com.almagest_dev.tacobank_core_server.domain.member.repository.MemberRepository;
import com.almagest_dev.tacobank_core_server.infrastructure.external.testbed.client.TestbedApiClient;
import com.almagest_dev.tacobank_core_server.presentation.dto.transantion.TransactionApiRequestDto2;
import com.almagest_dev.tacobank_core_server.presentation.dto.transantion.TransactionApiResponseDto2;
import com.almagest_dev.tacobank_core_server.presentation.dto.transantion.TransactionListRequestDto2;
import com.almagest_dev.tacobank_core_server.presentation.dto.transantion.TransactionResponseDto2;
import com.almagest_dev.tacobank_core_server.presentation.dto.transantion.Transaction;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TestbedApiClient testbedApiClient;
    private final MemberRepository memberRepository;

    /**
     * 거래 내역 조회 서비스
     */
    @Transactional
    public List<TransactionResponseDto2> getTransactionList(TransactionListRequestDto2 requestDto) {
        // 멤버 조회
        Member member = memberRepository.findById(requestDto.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("멤버를 찾을 수 없습니다."));

        String userFinanceId = member.getUserFinanceId();

        if (userFinanceId == null || userFinanceId.isEmpty()) {
            throw new IllegalArgumentException("회원의 금융 ID가 존재하지 않습니다. 계좌 조회를 먼저 진행하세요.");
        }

        // API 호출 요청 생성 및 데이터 조회
        TransactionApiResponseDto2 responseDto = fetchTransactionListFromApi(userFinanceId, requestDto);

        // 응답 매핑
        return mapToTransactionResponseDto(responseDto.getTransactions());
    }

    private TransactionApiResponseDto2 fetchTransactionListFromApi(String userFinanceId, TransactionListRequestDto2 requestDto) {
        // API 요청 DTO 생성
        TransactionApiRequestDto2 apiRequestDto = new TransactionApiRequestDto2();
        apiRequestDto.setFintechUseNum(userFinanceId); // 금융 ID
        apiRequestDto.setInquiryType("A"); // 기본값: 전체 조회
        apiRequestDto.setInquiryBase("D"); // 기본값: 날짜 기준
        apiRequestDto.setFromDate(requestDto.getFromDate()); // 조회 시작 날짜
        apiRequestDto.setFromTime("001000"); // 기본값
        apiRequestDto.setToDate(requestDto.getToDate()); // 조회 종료 날짜
        apiRequestDto.setToTime("240000"); // 기본값
        apiRequestDto.setSortOrder("D"); // 기본값: 내림차순
        apiRequestDto.setTranDtime("20241115120000"); // 디폴트 값
        apiRequestDto.setDataLength("2"); // 기본값

        // API 호출
        return testbedApiClient.requestApi(
                apiRequestDto,
                "/openbank/tranlist",
                TransactionApiResponseDto2.class
        );
    }

    private List<TransactionResponseDto2> mapToTransactionResponseDto(List<Transaction> transactions) {
        // API 응답 데이터를 클라이언트로 반환하기 위한 DTO로 매핑
        return transactions.stream()
                .map(transaction -> new TransactionResponseDto2(
                        transaction.getTranNum(),
                        transaction.getType(),
                        transaction.getPrintContent(),
                        transaction.getAmount(),
                        transaction.getAfterBalanceAmount(),
                        transaction.getTranDateTime()
                ))
                .collect(Collectors.toList());
    }


}
