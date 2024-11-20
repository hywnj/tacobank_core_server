package com.almagest_dev.tacobank_core_server.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AccountDto {
    private String accountHolder;
    private String accountNum;
    private String bankName;
}
