package com.almagest_dev.tacobank_core_server.presentation.dto.transantion;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransactionApiRequestDto2 {
    private String fintechUseNum; // 금융 ID
    private String inquiryType; // 조회 타입 (기본값: "A")
    private String inquiryBase; // 조회 기준 (기본값: "D")
    private String fromDate; // 조회 시작 날짜
    private String fromTime; // 기본값: "001000"
    private String toDate; // 조회 종료 날짜
    private String toTime; // 기본값: "240000"
    private String sortOrder; // 정렬 순서 (기본값: "D")
    private String tranDtime; // 디폴트 값
    private String dataLength; // 기본값: "null"
}
