package com.almagest_dev.tacobank_core_server.presentation.controller;


import com.almagest_dev.tacobank_core_server.application.service.SettlementService;
import com.almagest_dev.tacobank_core_server.common.dto.CoreResponseDto;
import com.almagest_dev.tacobank_core_server.presentation.dto.notify.NotificationResponseDto;
import com.almagest_dev.tacobank_core_server.presentation.dto.settlement.*;
import jakarta.validation.Valid;
import lombok.Getter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Getter
@RestController
@RequestMapping("/core/settlement")
public class SettlementController {

    private final SettlementService settlementService;
    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    // 송금 요청하기
    @PostMapping("/request")
    public ResponseEntity<List<NotificationResponseDto>> requestSettlement(@RequestBody SettlementRequestDto request) {
        List<NotificationResponseDto> response = settlementService.processSettlementRequest(request);
        return ResponseEntity.ok(response);
    }

    // 정산 현황 조회
    @GetMapping("/status/{memberId}")
    public ResponseEntity<SettlementStatusResponseDto> getSettlementStatus(@PathVariable Long memberId) {
        SettlementStatusResponseDto settlementStatus = settlementService.getSettlementStatus(memberId);
        return ResponseEntity.ok(settlementStatus);
    }

    // 정산 상세내역 조회
    @GetMapping("/{settlementId}/details/{memberId}")
    public ResponseEntity<SettlementDetailsListResponseDto> getSettlementDetails(
            @PathVariable Long settlementId,
            @PathVariable Long memberId
    ) {
        SettlementDetailsListResponseDto response = settlementService.getSettlementDetails(settlementId, memberId);
        return ResponseEntity.ok(response);
    }

    // 독촉 알림 보내기
    @PostMapping("/{settlementId}/notify/{memberId}")
    public ResponseEntity<NotificationResponseDto> notifyPendingSettlementForMember(
            @PathVariable Long settlementId,
            @PathVariable Long memberId
    ) {
        NotificationResponseDto response = settlementService.notifyPendingSettlementForMember(settlementId, memberId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/transfers")
    public ResponseEntity<?> validateSettlementsAndGetAvailableBalances(@RequestBody @Valid SettlementTransferRequestDto requestDto) {
        SettlementTransferResponseDto response = settlementService.validateSettlementsAndGetAvailableBalances(requestDto);
        return ResponseEntity.ok(new CoreResponseDto<>("success", "사용자 출금 가능 계좌 잔액조회 성공", response));
    }

}
