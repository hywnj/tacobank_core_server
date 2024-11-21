package com.almagest_dev.tacobank_core_server.presentation.dto;


import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder(toBuilder = true)
public class ReceiptOcrResponseDto {
    private final int totalAmount;  // 총합계 금액
    private final List<Item> items; // 품목 리스트

    @Getter
    @Builder
    public static class Item {
        private final int number;       // 품목 번호
        private final String name;      // 품목 이름
        private final int totalPrice;   // 품목총 가격
    }
}
