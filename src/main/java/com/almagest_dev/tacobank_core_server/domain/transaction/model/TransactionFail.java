
package com.almagest_dev.tacobank_core_server.domain.transaction.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "transaction_fail")
public class TransactionFail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", columnDefinition = "BIGINT NOT NULL COMMENT '거래 요청 ID'")
    private TransactionRequest transactionRequest;

    @Column(columnDefinition = "VARCHAR(40) NOT NULL COMMENT '거래 고유 ID'")
    private String transactionId;

    @Column(columnDefinition = "BIGINT COMMENT '정산 ID'")
    private Long settlementId;

    @Column(columnDefinition = "BIGINT COMMENT '출금 멤버 ID'")
    private Long memberId;

    @Column(columnDefinition = "BIGINT COMMENT '출금 계좌 ID'")
    private Long accountId;

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

    @Column(columnDefinition = "VARCHAR(10) COMMENT '응답 코드'")
    private String responseCode;

    @Column(columnDefinition = "VARCHAR(255) COMMENT '응답 메시지'")
    private String responseMassage;

    @Column(columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '거래 실패일시'")
    private LocalDateTime transactionDate;
}
