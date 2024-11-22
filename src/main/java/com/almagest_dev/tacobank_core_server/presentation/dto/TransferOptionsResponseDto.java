package com.almagest_dev.tacobank_core_server.presentation.dto;

import com.almagest_dev.tacobank_core_server.presentation.dto.account.AccountDto;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class TransferOptionsResponseDto {
    private List<AccountDto> favoriteAccounts;
    private List<AccountDto> recentAccounts;
}