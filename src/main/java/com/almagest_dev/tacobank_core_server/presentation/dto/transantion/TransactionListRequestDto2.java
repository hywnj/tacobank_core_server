package com.almagest_dev.tacobank_core_server.presentation.dto.transantion;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransactionListRequestDto2 {
    private Long memberId; // 회원 ID
    private String fromDate; // 조회 시작 날짜
    private String toDate; // 조회 종료 날짜
}
