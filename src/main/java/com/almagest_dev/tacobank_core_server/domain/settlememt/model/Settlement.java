package com.almagest_dev.tacobank_core_server.domain.settlememt.model;

import com.almagest_dev.tacobank_core_server.domain.account.model.Account;
import com.almagest_dev.tacobank_core_server.domain.group.model.Group;
import com.almagest_dev.tacobank_core_server.domain.receipt.model.Receipt;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "settlement")
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pay_group_id", columnDefinition = "BIGINT NOT NULL COMMENT '정산그룹 ID'")
    private Group payGroup;

    @Column(name = "receipt_id", columnDefinition = "BIGINT COMMENT '영수증 ID'")
    private Long receiptId;

//    @Column(name = "settlement_account_id", columnDefinition = "BIGINT COMMENT '정산받을 계좌아이디'")
//    private Long settlementAccountId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_account_id", columnDefinition = "BIGINT COMMENT '정산 받을 계좌 ID'")
    private Account settlementAccount;

    @Column(name = "settlement_total_amount", columnDefinition = "INT COMMENT '정산총액'")
    private Integer settlementTotalAmount;

    @Column(name = "settlement_status", columnDefinition = "VARCHAR(1) COMMENT '정산상태'")
    private String settlementStatus;

    @Column(columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '생성일자'")
    private LocalDateTime createdDate;

    @Column(columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL COMMENT '수정일자'")
    private LocalDateTime updatedDate;

    @OneToMany(mappedBy = "settlement")
    private List<Receipt> receipts;

    @PrePersist
    public void prePersist() {
        this.createdDate = LocalDateTime.now();
        this.updatedDate = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedDate = LocalDateTime.now();
    }


    public void setPayGroup(Group group) {
        this.payGroup = group;
    }

    public void setSettlementAccount(Account selectedAccount) {
        this.settlementAccount = selectedAccount;
    }

    public void setSettlementTotalAmount(int totalAmount) {
        this.settlementTotalAmount = totalAmount;
    }

    public void setSettlementStatus(String n) {
        this.settlementStatus = n;
    }
}
