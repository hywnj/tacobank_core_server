package com.almagest_dev.tacobank_core_server.presentation.dto.account;

import com.almagest_dev.tacobank_core_server.presentation.dto.transfer.TransactionResponseDto;
import lombok.Data;
import lombok.Setter;

import java.util.List;

@Data
public class AccountResponseDto {
    private Long accountId; // 계좌 식별자
    private String accountName; // 계좌명
    private String accountNum; // 계좌 번호
    private String accountHolder; // 예금주
    private String bankName; // 은행 이름
    private Double balance; // 잔액
    @Setter
    private List<TransactionResponseDto> transactionList; // 거래 내역 리스트

}