package com.almagest_dev.tacobank_core_server.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class TransferOptionsResponseDto {
    private List<AccountDto> favoriteAccounts;
    private List<AccountDto> recentAccounts;
}