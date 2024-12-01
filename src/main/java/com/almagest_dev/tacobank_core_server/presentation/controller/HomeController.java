package com.almagest_dev.tacobank_core_server.presentation.controller;

import com.almagest_dev.tacobank_core_server.application.service.HomeService;
import com.almagest_dev.tacobank_core_server.presentation.dto.home.AccountMemberReponseDto;
import com.almagest_dev.tacobank_core_server.presentation.dto.home.AccountRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/taco/core/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    /**
     * 사용자 계좌 및 거래 내역 조회
     */
    @PostMapping("/accounts")
    public ResponseEntity<AccountMemberReponseDto> getUserAccounts(@RequestBody AccountRequestDto requestDto) {
        AccountMemberReponseDto responseDto = homeService.getUserAccounts(requestDto);
        return ResponseEntity.ok(responseDto);
    }


}
