package com.almagest_dev.tacobank_core_server.presentation.controller;

import com.almagest_dev.tacobank_core_server.application.service.ReceiptOcrService;
import com.almagest_dev.tacobank_core_server.presentation.dto.ReceiptOcrResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/core/receipts")
@RequiredArgsConstructor
public class ReceiptOcrController {
    private final ReceiptOcrService receiptOcrService;

    @PostMapping("/ocr")
    public ResponseEntity<?> extractDataFromReceiptImage(@RequestParam("file") MultipartFile file) {
        ReceiptOcrResponseDto response = receiptOcrService.processReceiptOcr(file);
        return ResponseEntity.ok(response);
    }
}
