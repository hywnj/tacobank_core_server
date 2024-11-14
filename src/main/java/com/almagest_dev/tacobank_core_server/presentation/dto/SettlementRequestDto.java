package com.almagest_dev.tacobank_core_server.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SettlementRequestDto {
    private Long groupId; // 정산 그룹 ID
    private Integer totalAmount; // 총 정산 금액
    private Long accountId;
}
