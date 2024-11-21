package com.almagest_dev.tacobank_core_server.presentation.controller;


import com.almagest_dev.tacobank_core_server.application.service.SettlementService;
import com.almagest_dev.tacobank_core_server.common.dto.CoreResponseDto;
import com.almagest_dev.tacobank_core_server.presentation.dto.*;
import com.almagest_dev.tacobank_core_server.presentation.dto.settlement.SettlementRequestDto;
import jakarta.validation.Valid;
import lombok.Getter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Getter
@RestController
@RequestMapping("/core/settlement")
public class SettlementController {

    private final SettlementService settlementService;
    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @PostMapping("/request")
    public ResponseEntity<String> requestSettlement(@RequestBody SettlementRequestDto request) {
        settlementService.processSettlementRequest(request);
        return ResponseEntity.ok("정산 요청이 완료되었습니다.");
    }


    @GetMapping("/status/{memberId}")
    public ResponseEntity<SettlementStatusResponseDto> getSettlementStatus(@PathVariable Long memberId) {
        SettlementStatusResponseDto settlementStatus = settlementService.getSettlementStatus(memberId);
        return ResponseEntity.ok(settlementStatus);
    }

    @GetMapping("/{settlementId}/details/{memberId}")
    public ResponseEntity<SettlementDetailsListResponseDto> getSettlementDetails(
            @PathVariable Long settlementId,
            @PathVariable Long memberId
    ) {
        SettlementDetailsListResponseDto response = settlementService.getSettlementDetails(settlementId, memberId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{settlementId}/notify/{memberId}")
    public ResponseEntity<String> notifyPendingSettlementForMember(
            @PathVariable Long settlementId,
            @PathVariable Long memberId
    ) {
        settlementService.notifyPendingSettlementForMember(settlementId, memberId);
        return ResponseEntity.ok("해당 사용자에게 정산 알림을 보냈습니다.");
    }

    @PostMapping("/transfers")
    public ResponseEntity<?> validateSettlementsAndGetAvailableBalances(@RequestBody @Valid SettlementTransferRequestDto requestDto) {
        SettlementTransferResponseDto response = settlementService.validateSettlementsAndGetAvailableBalances(requestDto);
        return ResponseEntity.ok(new CoreResponseDto<>("success", "사용자 출금 가능 계좌 잔액조회 성공", response));
    }

}
