package com.almagest_dev.tacobank_core_server.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class SettlementStatusResponseDto {
    private List<MyCreatedSettlementDto> createdSettlements;
    private List<MyIncludedSettlementDto> includedSettlements;
    private List<AccountDto> availableAccounts;
}

