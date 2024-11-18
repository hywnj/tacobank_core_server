package com.almagest_dev.tacobank_core_server.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TransferResponseDto {
    private String idempotencyKey;          // 중복 방지 키
    private String tranDtm;                 // 송금 일시
    private Long memberId;                  // 송금하는 사용자 ID (출금 사용자)
    private Long accountId;                 // 출금 계좌 ID
    private String depositAccountNum;       // 출금 계좌 번호
    private String depositAccountHolder;    // 출금 예금주
    private String depositBankCode;         // 출금 은행 코드
    private String receiverAccountNum;      // 입금(수취) 계좌 번호
    private String receiverAccountHolder;   // 입금(수취) 예금주(수취인)
    private String receiverBankCode;        // 입금(수취) 은행 코드
    private Integer amount;                 // 송금액

    public static TransferResponseDto create(
            String idempotencyKey,
            String apiTranDtm,
            Long memberId,
            Long accountId,
            String depositAccountNum,
            String depositAccountHolder,
            String depositBankCode,
            String receiverAccountNum,
            String receiverAccountHolder,
            String receiverBankCode,
            Integer amount
    ) {
        // 포맷 변경
        String formattedTranDtm = LocalDateTime.parse(apiTranDtm.substring(0, 14), // 초 단위까지만 파싱
                        DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        return new TransferResponseDto(
                idempotencyKey,
                formattedTranDtm,
                memberId,
                accountId,
                depositAccountNum,
                depositAccountHolder,
                depositBankCode,
                receiverAccountNum,
                receiverAccountHolder,
                receiverBankCode,
                amount
        );
    }
}
