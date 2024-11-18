package com.almagest_dev.tacobank_core_server.infrastructure.client.dto;

import lombok.Data;

import java.util.List;

@Data
public class NaverReceiptOcrResponseDto {
    private String version;     // 버전 정보 (V2 만)
    private String requestId;   // API 호출 UUID
    private Long timestamp;   // API 호출 시각(Timestamp) (1724976333747)
    private List<NaverOcrImage> images; // image 세부내용
}
