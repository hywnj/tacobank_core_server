package com.almagest_dev.tacobank_core_server.presentation.controller;

import com.almagest_dev.tacobank_core_server.application.service.TransferService;
import com.almagest_dev.tacobank_core_server.presentation.dto.ReceiverInquiryRequestDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/core/transfers")
public class TransferController {
    private final TransferService transferService;

    /**
     * 송금할 은행, 계좌번호 입력 후 수취인 조회
     */
    @PostMapping("/receiver")
    public ResponseEntity<?> inquireReceiverAccount(@RequestBody @Valid ReceiverInquiryRequestDto requestDto) {
        transferService.inquireReceiverAccount(requestDto);
        return ResponseEntity.ok(null);
    }
}
