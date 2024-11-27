package com.almagest_dev.tacobank_core_server.presentation.dto.transantion;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    private Long tranNum; // 거래 고유 시퀀스 넘버
    private String type; // 거래 타입 (입금/출금)
    private String printContent; // 입출금 내용
    private Double amount; // 거래 금액
    private Double afterBalanceAmount; // 거래 후 잔액
    private String tranDateTime; // 거래 일시 (YYYY.mm.dd HH:mm:ss)
}
