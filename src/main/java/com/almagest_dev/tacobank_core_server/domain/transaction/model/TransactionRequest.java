package com.almagest_dev.tacobank_core_server.domain.transaction.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "transaction_request")
public class TransactionRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "VARCHAR(40) NOT NULL COMMENT '거래 고유 ID'")
    private String transactionId;

    @Column(columnDefinition = "VARCHAR(40) NOT NULL COMMENT '중복 방지 KEY(클라이언트에서 생성)'")
    private String idempotencyKey;

    @Column(columnDefinition = "VARCHAR(40) NOT NULL COMMENT '테스트베드 API 요청 ID'")
    private String apiTranId;

    @Column(columnDefinition = "BIGINT COMMENT '정산 ID'")
    private Long settlementId;

    @Column(columnDefinition = "BIGINT COMMENT '출금 멤버 ID'")
    private Long memberId;

    @Column(columnDefinition = "BIGINT COMMENT '출금 계좌 ID'")
    private Long accountId;

    @Column(columnDefinition = "VARCHAR(5) NOT NULL COMMENT '출금 은행코드'")
    private String depositBankCode;

    @Column(columnDefinition = "VARCHAR(20) NOT NULL COMMENT '출금 계좌번호'")
    private String depositAccountNum;

    @Column(columnDefinition = "VARCHAR(20) NOT NULL COMMENT '출금 예금주'")
    private String depositAccountHolder;

    @Column(columnDefinition = "VARCHAR(10) NOT NULL COMMENT '입금인자 출력문구'")
    private String printContent;

    @Column(columnDefinition = "VARCHAR(5) NOT NULL COMMENT '수신 은행코드'")
    private String receiverBankCode;

    @Column(columnDefinition = "VARCHAR(20) NOT NULL COMMENT '수신 계좌번호'")
    private String receiverAccountNum;

    @Column(columnDefinition = "VARCHAR(20) NOT NULL COMMENT '수신 예금주'")
    private String receiverAccountHolder;

    @Column(columnDefinition = "INT NOT NULL COMMENT '거래 금액'")
    private Integer amount;

    @Column(columnDefinition = "VARCHAR(1) DEFAULT 'R' NOT NULL COMMENT '거래 요청 상태(R, S, F)'")
    private String status;

    @Column(columnDefinition = "VARCHAR(10) COMMENT '응답 코드'")
    private String responseCode;

    @Column(columnDefinition = "VARCHAR(255) COMMENT '응답 메시지'")
    private String responseMassage;

    @Column(columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '거래 요청일시'")
    private LocalDateTime requestedDate;

    @Column(columnDefinition = "DATETIME ON UPDATE CURRENT_TIMESTAMP COMMENT '거래 응답일시'")
    private LocalDateTime responseDate;

}
