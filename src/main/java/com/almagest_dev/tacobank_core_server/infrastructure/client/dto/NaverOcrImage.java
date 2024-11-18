package com.almagest_dev.tacobank_core_server.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
public class NaverOcrImage {
    private String uid; // 영수증 이미지 UID
    private String name; // 영수증 이미지 이름
    private String inferResult; // 인식 결과
    private String message; // 결과 메시지
    private ValidationResult validationResult; // 유효성 검사 결과 정보
    private ConvertedImageInfo convertedImageInfo; // 변환 이미지 정보
    private Receipt receipt; // 영수증 세부 정보

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL) // null인 필드 제외
    public static class ValidationResult {
        private String result; // 유효성 검사 결과 코드
        private String message; // 유효성 검사 결과 세부 메시지
    }

    @Data
    public static class ConvertedImageInfo {
        private Integer width; // 변환 이미지 가로 길이
        private Integer height; // 변환 이미지 세로 길이
        private Integer pageIndex; // 변환 이미지 페이지 인덱스
    }


    @Data
    public static class Receipt {
        private Meta meta; // 메타 정보
        private ReceiptResult result; // 영수증 OCR 인식 결과

        @Data
        public static class Meta {
            private String estimatedLanguage; // OCR 추정 언어
        }
    }
}
