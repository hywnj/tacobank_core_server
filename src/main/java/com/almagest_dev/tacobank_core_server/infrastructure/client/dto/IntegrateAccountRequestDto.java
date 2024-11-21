package com.almagest_dev.tacobank_core_server.infrastructure.client.dto;

import lombok.Data;

@Data
public class IntegrateAccountRequestDto {
    private String userFinanceId;// 사용자 금융 ID
    private String userName;
    private String inquiryBankType;
    private String traceNo; // 추적 번호
}
// 통합 계좌 조회 요청 DTO