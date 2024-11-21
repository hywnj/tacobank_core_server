package com.almagest_dev.tacobank_core_server.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class SettlementTransferResponseDto {
    private String idempotencyKey; // 중복 방지 키
    private List<AccountBalance> accountBalances; // 계좌 잔액
}
