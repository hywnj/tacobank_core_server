package com.almagest_dev.tacobank_core_server.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransferSessionData {
    private String depositAccountNum;       // 출근 계좌 번호
    private String depositAccountHolder;    // 출금 예금주
    private String depositBankCode;         // 출금 은행 코드
    private String receiverAccountNum;      // 입금(수취) 계좌 번호
    private String receiverAccountHolder;   // 입금(수취) 예금주(수취인)
    private String receiverBankCode;        // 입금(수취) 은행 코드
    private String amount;                  // 송금액
}
