package com.almagest_dev.tacobank_core_server.presentation.dto;

import lombok.Getter;

@Getter
public class SettlementDetailsResponseDto {
    private final Long memberId;
    private final Integer settlementAmount;
    private final String settlementStatus;

    public SettlementDetailsResponseDto(Long memberId, Integer settlementAmount, String settlementStatus) {
        this.memberId = memberId;
        this.settlementAmount = settlementAmount;
        this.settlementStatus = settlementStatus;
    }


}
