package com.almagest_dev.tacobank_core_server.presentation.controller;


import com.almagest_dev.tacobank_core_server.application.service.SettlementService;
import com.almagest_dev.tacobank_core_server.presentation.dto.SettlementDetailsResponseDto;
import com.almagest_dev.tacobank_core_server.presentation.dto.SettlementRequestDto;
import com.almagest_dev.tacobank_core_server.presentation.dto.SettlementResponseDto;
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

    @PostMapping("/calculate")
    public List<SettlementResponseDto> calculateSettlement(@RequestBody SettlementRequestDto request) {
        return settlementService.calculateSettlement(request);
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

}
