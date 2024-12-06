package com.almagest_dev.tacobank_core_server.presentation.dto.transfer;

import com.almagest_dev.tacobank_core_server.presentation.dto.account.AccountDto;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class TransferOptionsResponseDto {
    private List<AccountDto> favoriteAccounts;  // 즐겨찾는 계좌 리스트
    private List<AccountDto> recentAccounts;    // 최근 이체 계좌 리스트
    private List<AccountDto> friendsAccounts;   // 친구 메인 계좌 리스트
}