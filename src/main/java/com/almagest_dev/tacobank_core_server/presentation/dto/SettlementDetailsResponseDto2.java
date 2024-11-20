package com.almagest_dev.tacobank_core_server.presentation.dto;

import lombok.Data;

@Data
public class SettlementDetailsResponseDto2 {
    private Long groupMemberId;
    private String groupMemberName;
    private Long groupMemberAmount;
    private String groupMemberStatus;

    public SettlementDetailsResponseDto2(Long groupMemberId, String groupMemberName, Long groupMemberAmount, String groupMemberStatus) {
        this.groupMemberId = groupMemberId;
        this.groupMemberName = groupMemberName;
        this.groupMemberAmount = groupMemberAmount;
        this.groupMemberStatus = groupMemberStatus;
    }

}
