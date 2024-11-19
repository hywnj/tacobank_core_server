package com.almagest_dev.tacobank_core_server.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TransferRequestDto {
    @NotBlank(message = "중복 방지 키를 보내주세요.")
    private String idempotencyKey;          // 중복 방지 키

    @NotNull(message = "출금 사용자 ID를 보내주세요.")
    private Long withdrawalMemberId;           // 송금하는 사용자 ID (출금 사용자)
    @NotNull(message = "출금 계좌 ID를 보내주세요.")
    private Long withdrawalAccountId;          // 출금 계좌 ID

    @NotBlank(message = "출금 계좌 번호를 보내주세요.")
    private String withdrawalAccountNum;       // 출금 계좌 번호
    @NotBlank(message = "출금 예금주 정보를 보내주세요.")
    private String withdrawalAccountHolder;    // 출금 예금주
    @NotBlank(message = "출금 은행 코드를 보내주세요.")
    private String withdrawalBankCode;         // 출금 은행 코드

    @NotBlank(message = "입금 계좌 번호를 보내주세요.")
    private String receiverAccountNum;      // 입금(수취) 계좌 번호
    @NotBlank(message = "입금 예금주 정보를 보내주세요.")
    private String receiverAccountHolder;   // 입금(수취) 예금주(수취인)
    @NotBlank(message = "입금 은행 코드를 보내주세요.")
    private String receiverBankCode;        // 입금(수취) 은행 코드

    @NotNull(message = "송금액을 보내주세요.")
    private int amount;                 // 송금액

    private String rcvPrintContent;     // 입금계좌 인자내역
    private String wdPrintContent;      // 출금계좌 인자내역
}
