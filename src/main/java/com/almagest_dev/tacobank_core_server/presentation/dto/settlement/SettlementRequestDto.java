package com.almagest_dev.tacobank_core_server.presentation.dto.settlement;

import lombok.Data;

import java.util.List;

@Data
public class SettlementRequestDto {
    private Long leaderId; // 그룹장 ID
    private Long groupId; // 선택된 그룹 ID (null이면 친구 선택)
    private int totalAmount; // 정산 총액
    private List<Long> friendIds; // 친구 선택 시 친구 ID 목록
    private Long settlementAccountId; // 선택된 계좌 ID
    private List<SettlementMemberDto> memberAmounts; // 멤버별 금액 리스트
}
