package com.almagest_dev.tacobank_core_server.infrastructure.client.dto;

import lombok.Data;

import java.util.List;

@Data
public class ReceiptResult {

    private StoreInfo storeInfo;              // 점포 정보
    private List<SubResult> subResults;       // 상품 그룹 정보
    private SubTotal subTotal;                // 총합계 정보
    private TotalPrice totalPrice;            // 총 금액 정보


    @Data
    public static class StoreInfo {
        private List<Address> addresses;      // 점포 주소 상세 정보
        private List<Tel> tel;                // 점포 전화번호 상세 정보

        @Data
        public static class Address {
            private String text;              // 인식된 텍스트
            private Formatted formatted;      // 인식된 텍스트 정보
            private String keyText;           // 텍스트 키 값
            private Float confidenceScore;    // 신뢰도
            private List<BoundingPoly> boundingPolys; // Bounding Poly 세부 정보
            private List<MaskingPoly> maskingPolys;   // Masking Poly 세부 정보

            @Data
            public static class Formatted {
                private String value;         // 인식된 텍스트 값
            }
        }

        @Data
        public static class Tel {
            private String text;              // 인식된 텍스트
            private Formatted formatted;      // 인식된 텍스트 정보
            private String keyText;           // 텍스트 키 값
            private Float confidenceScore;    // 신뢰도
            private List<BoundingPoly> boundingPolys; // Bounding Poly 세부 정보
            private List<MaskingPoly> maskingPolys;   // Masking Poly 세부 정보

            @Data
            public static class Formatted {
                private String value;         // 인식된 텍스트 값
            }
        }
    }

    @Data
    public static class SubResult {
        private List<Item> items;             // 인식된 상품 상세 정보

        @Data
        public static class Item {
            private Detail name;              // 항목 이름 상세 정보
            private Detail code;              // 항목 코드 상세 정보
            private Detail count;             // 항목 수량 상세 정보
            private Price price;              // 항목 가격 정보

            @Data
            public static class Detail {
                private String text;          // 인식된 텍스트
                private Formatted formatted;  // 인식된 텍스트 정보
                private String keyText;       // 텍스트 키 값
                private Float confidenceScore; // 신뢰도
                private List<BoundingPoly> boundingPolys; // Bounding Poly 세부 정보
                private List<MaskingPoly> maskingPolys;   // Masking Poly 세부 정보

                @Data
                public static class Formatted {
                    private String value;     // 인식된 텍스트 값
                }
            }

            @Data
            public static class Price {
                private Detail price;         // 항목 가격 상세 정보
                private Detail unitPrice;     // 항목 단가 상세 정보
            }
        }
    }

    @Data
    public static class SubTotal {
        private List<TaxPrice> taxPrice;         // 부과세 상세 정보
        private List<DiscountPrice> discountPrice; // 할인 금액 상세 정보

        @Data
        public static class TaxPrice {
            private String text;                 // 인식된 텍스트
            private Formatted formatted;         // 인식된 텍스트 정보
            private String keyText;              // 텍스트 키 값
            private Float confidenceScore;       // 신뢰도
            private List<BoundingPoly> boundingPolys; // Bounding Poly 세부 정보
            private List<MaskingPoly> maskingPolys;   // Masking Poly 세부 정보

            @Data
            public static class Formatted {
                private String value;            // 인식된 텍스트 값
            }
        }

        @Data
        public static class DiscountPrice {
            private String text;                 // 인식된 텍스트
            private Formatted formatted;         // 인식된 텍스트 정보
            private String keyText;              // 텍스트 키 값
            private Float confidenceScore;       // 신뢰도
            private List<BoundingPoly> boundingPolys; // Bounding Poly 세부 정보
            private List<MaskingPoly> maskingPolys;   // Masking Poly 세부 정보

            @Data
            public static class Formatted {
                private String value;            // 인식된 텍스트 값
            }
        }
    }

    @Data
    public static class TotalPrice {
        private Detail price;                   // 총 금액 상세 정보

        @Data
        public static class Detail {
            private String text;                // 인식된 텍스트
            private Formatted formatted;        // 인식된 텍스트 정보
            private String keyText;             // 텍스트 키 값
            private Float confidenceScore;      // 신뢰도
            private List<BoundingPoly> boundingPolys; // Bounding Poly 세부 정보
            private List<MaskingPoly> maskingPolys;   // Masking Poly 세부 정보

            @Data
            public static class Formatted {
                private String value;           // 인식된 텍스트 값
            }
        }
    }

    @Data
    public static class BoundingPoly {
        private List<BoundingPolyVertex> vertices; // Bounding Poly의 좌표 정보

        @Data
        public static class BoundingPolyVertex {
            private Float x;                     // X축 좌표
            private Float y;                     // Y축 좌표
        }
    }

    @Data
    public static class MaskingPoly {
        private List<BoundingPolyVertex> vertices; // Masking Poly의 좌표 정보

        @Data
        public static class BoundingPolyVertex {
            private Float x;                     // X축 좌표
            private Float y;                     // Y축 좌표
        }
    }
}
