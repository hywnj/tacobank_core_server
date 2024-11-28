package com.almagest_dev.tacobank_core_server.presentation.dto.home;

import lombok.Data;

@Data
public class TransactionResponseDto2 {
    private Long tranNum; // 거래 고유 시퀀스 번호
    private String printContent; // 입금출력내용
    private String tranDateTime; // 거래 일시 (YYYY.MM.DD HH:mm:ss)
    private String tranAmt;

}
