package com.almagest_dev.tacobank_core_server.presentation.dto;

import lombok.Data;

@Data
public class TransferResponseDto {
    private String idempotencyKey;          // 중복 방지 키
    private Long memberId;                  // 송금하는 사용자 ID (출금 사용자)
    private Long accountId;                 // 출금 계좌 ID
    private String depositAccountNum;       // 출금 계좌 번호
    private String depositAccountHolder;    // 출금 예금주
    private String depositBankCode;         // 출금 은행 코드
    private String receiverAccountNum;      // 입금(수취) 계좌 번호
    private String receiverAccountHolder;   // 입금(수취) 예금주(수취인)
    private String receiverBankCode;        // 입금(수취) 은행 코드
    private Integer amount;                 // 송금액
    private String printContent;            // 입금자 출력 문구
}
