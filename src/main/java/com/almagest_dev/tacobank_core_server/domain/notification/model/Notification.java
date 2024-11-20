package com.almagest_dev.tacobank_core_server.domain.notification.model;

import com.almagest_dev.tacobank_core_server.domain.member.model.Member;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", columnDefinition = "BIGINT NOT NULL COMMENT '멤버 ID'")
    private Member member;

    @Column(columnDefinition = "VARCHAR(255) COMMENT '알림 내용'")
    private String message;

    @Column(columnDefinition = "VARCHAR(1) COMMENT '알림 확인 여부'")
    private String isRead;

    @Column(columnDefinition = "DATETIME NOT NULL COMMENT '등록일자'")
    private LocalDateTime createdDate;

    @PrePersist
    protected void onCreate() {
        this.createdDate = LocalDateTime.now();
    }

}
