package com.almagest_dev.tacobank_core_server.presentation.controller;


import com.almagest_dev.tacobank_core_server.application.service.SettlementService;
import com.almagest_dev.tacobank_core_server.presentation.dto.*;
import com.almagest_dev.tacobank_core_server.presentation.dto.settlement.SettlementRequestDto;
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

    @PostMapping("/request")
    public ResponseEntity<String> requestSettlement(@RequestBody SettlementRequestDto request) {
        settlementService.processSettlementRequest(request);
        return ResponseEntity.ok("정산 요청이 완료되었습니다.");
    }

    @GetMapping("/{groupId}/details")
    public List<SettlementDetailsResponseDto> getSettlementDetailsByGroupId(@PathVariable Long groupId) {
        return settlementService.getSettlementDetailsByGroupId(groupId);
    }

    @GetMapping("/details/{groupId}/{memberId}")
    public ResponseEntity<List<SettlementDetailsResponseDto>> getSettlementDetailsForMember(
            @PathVariable Long groupId,
            @PathVariable Long memberId) {

        List<SettlementDetailsResponseDto> response = settlementService.getSettlementDetailsForMember(groupId, memberId);
        return ResponseEntity.ok(response);
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

    @PostMapping("/{settlementId}/notify")
    public ResponseEntity<String> notifyPendingSettlements(@PathVariable Long settlementId) {
        settlementService.notifyPendingSettlements(settlementId);
        return ResponseEntity.ok("정산 완료되지 않은 사용자들에게 알림을 보냈습니다.");
    }

}
