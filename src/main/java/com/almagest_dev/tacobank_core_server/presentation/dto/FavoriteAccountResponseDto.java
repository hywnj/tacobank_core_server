package com.almagest_dev.tacobank_core_server.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FavoriteAccountResponseDto {
    private Long memberId;
    private String accountNumber;
    private String accountHolder;
    private String bankName; // 은행명

}
