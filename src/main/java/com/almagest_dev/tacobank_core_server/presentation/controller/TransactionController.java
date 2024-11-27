package com.almagest_dev.tacobank_core_server.presentation.controller;

import com.almagest_dev.tacobank_core_server.application.service.TransactionService;
import com.almagest_dev.tacobank_core_server.presentation.dto.transantion.TransactionListRequestDto2;
import com.almagest_dev.tacobank_core_server.presentation.dto.transantion.TransactionResponseDto2;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/taco/core/transfers")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * 거래 내역 조회
     */
    @PostMapping("/list")
    public ResponseEntity<List<TransactionResponseDto2>> getTransactionList(
            @RequestBody TransactionListRequestDto2 requestDto) {
        List<TransactionResponseDto2> transactionList = transactionService.getTransactionList(requestDto);
        return ResponseEntity.ok(transactionList);
    }
}
