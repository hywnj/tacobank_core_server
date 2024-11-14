package com.almagest_dev.tacobank_core_server.presentation.controller;

import com.almagest_dev.tacobank_core_server.application.service.AccountService;
import com.almagest_dev.tacobank_core_server.infrastructure.client.dto.AccountInfoDTO;
import com.almagest_dev.tacobank_core_server.infrastructure.client.dto.IntegrateAccountResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/core/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/user/{memberId}")
    public ResponseEntity<IntegrateAccountResponseDto> getUserAccounts(@PathVariable Long memberId) {
        IntegrateAccountResponseDto responseDto = accountService.getUserAccounts(memberId);
        return ResponseEntity.ok(responseDto);
    }


}