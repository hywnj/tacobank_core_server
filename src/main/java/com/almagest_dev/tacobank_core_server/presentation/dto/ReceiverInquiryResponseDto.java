package com.almagest_dev.tacobank_core_server.presentation.dto;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ReceiverInquiryResponseDto {
    private String idempotencyKey;  // 중복 방지 키
    private String printContent;    // 입금 인자 출력 내용
    private String receiverAccountHolder;    // 입금 예금주(수취인명)
}
