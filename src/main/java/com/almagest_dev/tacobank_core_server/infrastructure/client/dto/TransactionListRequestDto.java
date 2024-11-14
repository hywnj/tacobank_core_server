package com.almagest_dev.tacobank_core_server.infrastructure.client.dto;

import lombok.Data;

@Data
public class TransactionListRequestDto {
    private String fintechUseNum;
    private String inquiryType;
    private String inquiryBase;
    private String fromDate;
    private String fromTime;
    private String toDate;
    private String toTime;
    private String sortOrder;
    private String tranDtime;
}
// 거래내역 조회 DTO