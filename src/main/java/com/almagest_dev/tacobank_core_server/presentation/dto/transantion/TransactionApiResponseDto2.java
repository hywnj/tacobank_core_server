package com.almagest_dev.tacobank_core_server.presentation.dto.transantion;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class TransactionApiResponseDto2 {
    private String apiTranId; // 거래 ID
    private String apiTranDtm; // 거래 시간
    private String rspCode; // 응답 코드
    private String rspMessage; // 응답 메시지
    private List<Transaction> transactions;

}
