package com.almagest_dev.tacobank_core_server.domain.transfer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "transfer")
public class Transfer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "VARCHAR(40) NOT NULL COMMENT '거래 고유 ID'")
    private String transactionId;

    @Column(columnDefinition = "VARCHAR(40) NOT NULL COMMENT '중복 방지 KEY(클라이언트에서 생성)'")
    private String idempotencyKey;

    @Column(columnDefinition = "VARCHAR(40) NULL COMMENT '테스트베드 API 요청 ID'")
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

    /**
     * Transfer 관련 메서드
     */
    // Transfer 첫 생성 메서드
    public static Transfer createTransfer(String idempotencyKey,
                                          Long memberId, Long accountId,
                                          String depositBankCode, String depositAccountNum, String depositAccountHolder,
                                          String printContent,
                                          String receiverBankCode, String receiverAccountNum, String receiverAccountHolder,
                                          Integer amount) {
        return new Transfer(
                null, // id는 자동 생성
                UUID.randomUUID().toString(), // 고유 거래 ID
                idempotencyKey,               // 중복 방지 키
                null,                         // apiTranId 초기값 null
                null,                         // settlementId 초기값 null
                memberId,                     // 송금 요청자
                accountId,                    // 출금 계좌 ID
                depositBankCode,              // 출금 은행 코드
                depositAccountNum,            // 출금 계좌 번호
                depositAccountHolder,         // 출금 예금주
                printContent,                 // 입금인자 출력 문구
                receiverBankCode,             // 수신 은행 코드
                receiverAccountNum,           // 수신 계좌 번호
                receiverAccountHolder,        // 수신 예금주
                amount,                       // 송금 금액
                "R",                          // 초기 상태는 Requested
                null,                         // 응답 코드 초기값 null
                null,                         // 응답 메시지 초기값 null
                LocalDateTime.now(),          // 요청 일시
                null                          // 응답 일시 초기값 null
        );
    }
    // Transfer 업데이트 메서드
    public Transfer updateTransfer(String apiTranId, String status, String responseCode, String responseMessage, String responseDate) {
        return new Transfer(
                this.id,                      // 기존 ID 유지
                this.transactionId,           // 기존 거래 ID 유지
                this.idempotencyKey,          // 기존 중복 방지 키 유지
                apiTranId,               // 기존 API 요청 ID 유지
                this.settlementId,            // 기존 정산 ID 유지
                this.memberId,                // 기존 송금 요청자 유지
                this.accountId,               // 기존 출금 계좌 유지
                this.depositBankCode,         // 기존 출금 은행 유지
                this.depositAccountNum,       // 기존 출금 계좌 번호 유지
                this.depositAccountHolder,    // 기존 출금 예금주 유지
                this.printContent,            // 기존 입금인자 유지
                this.receiverBankCode,        // 기존 수신 은행 유지
                this.receiverAccountNum,      // 기존 수신 계좌 번호 유지
                this.receiverAccountHolder,   // 기존 수신 예금주 유지
                this.amount,                  // 기존 송금 금액 유지
                status,                       // 업데이트된 상태
                responseCode,                 // 업데이트된 응답 코드
                responseMessage,              // 업데이트된 응답 메시지
                this.requestedDate,           // 기존 요청 일시 유지
                responseDate != null
                        ? LocalDateTime.parse(responseDate, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS"))
                        : LocalDateTime.now()
        );
    }


}
