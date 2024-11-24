package com.almagest_dev.tacobank_core_server.domain.account.model;

import com.almagest_dev.tacobank_core_server.domain.member.model.Member;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;


@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "account")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", columnDefinition = "BIGINT NOT NULL COMMENT '멤버 ID'")
    private Member member;

    @Column(columnDefinition = "VARCHAR(20) COMMENT '계좌번호'")
    private String accountNumber;

    @Column(columnDefinition = "VARCHAR(255) COMMENT '예금주'")
    private String accountHolderName;

    @Column(columnDefinition = "VARCHAR(5) COMMENT '은행코드'")
    private String bankCode;

    @Column(columnDefinition = "VARCHAR(1) COMMENT '본인 인증 여부'")
    private String verificated;

    @Column(columnDefinition = "VARCHAR(40) COMMENT '계좌 핀테크 이용번호'")
    private String fintechUseNum;

    @Column(columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '등록일자'")
    private LocalDateTime createdDate;

    @Column(columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL COMMENT '수정일자'")
    private LocalDateTime updatedDate;

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public void setAccountHolderName(String accountHolderName) {
        this.accountHolderName = accountHolderName;
    }

    public void setBankCode(String bankCode) {
        this.bankCode = bankCode;
    }

    public void setFintechUseNum(String fintechUseNum) {
        this.fintechUseNum = fintechUseNum;
    }


    public void setMember(Member member) {
        this.member = member;
    }


    public void setVerificated() {
        this.verificated = "Y";
    }
}
