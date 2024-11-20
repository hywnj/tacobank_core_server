package com.almagest_dev.tacobank_core_server.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SettlementResponseDto {
    private Long memberId; // 멤버 ID
    private Integer amount;// 각 멤버의 분담 금액
    private String memberName;
}
