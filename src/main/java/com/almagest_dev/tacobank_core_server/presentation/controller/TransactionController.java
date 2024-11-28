package com.almagest_dev.tacobank_core_server.presentation.controller;

import com.almagest_dev.tacobank_core_server.application.service.TransactionService;
import com.almagest_dev.tacobank_core_server.presentation.dto.transantion.TransactionListRequestDto;
import com.almagest_dev.tacobank_core_server.presentation.dto.transantion.TransactionResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/taco/core/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * 거래 내역 조회
     */
    @PostMapping("/list")
    public ResponseEntity<List<TransactionResponseDto>> getTransactionList(
            @RequestBody TransactionListRequestDto requestDto) {
        List<TransactionResponseDto> transactionList = transactionService.getTransactionList(requestDto);
        return ResponseEntity.ok(transactionList);
    }
}
