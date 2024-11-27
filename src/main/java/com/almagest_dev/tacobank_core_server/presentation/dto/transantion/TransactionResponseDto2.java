package com.almagest_dev.tacobank_core_server.presentation.dto.transantion;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class TransactionResponseDto2 {
    private Long tranNum; // 거래 시퀀스 번호
    private String type; // 거래 타입 (입출금)
    private String printContent; // 입출금 내용
    private Double amount; // 거래 금액
    private Double afterBalanceAmount; // 거래 후 잔액
    private String tranDateTime; // 거래 일시 (YYYY.MM.DD HH:mm:ss)
}
