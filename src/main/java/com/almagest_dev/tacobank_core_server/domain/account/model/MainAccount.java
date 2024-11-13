package com.almagest_dev.tacobank_core_server.domain.account.model;

import com.almagest_dev.tacobank_core_server.domain.member.model.Member;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "main_account")
public class MainAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", columnDefinition = "BIGINT NOT NULL COMMENT '멤버 ID'")
    private Member member;

    @OneToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
}
