package com.almagest_dev.tacobank_core_server.domain.member.model;


import com.almagest_dev.tacobank_core_server.domain.group.model.Group;
import com.almagest_dev.tacobank_core_server.domain.group.model.GroupMember;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "member")
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", columnDefinition = "BIGINT COMMENT '권한 ID'")
    private Role role;

    @Column(columnDefinition = "VARCHAR(40) COMMENT '사용자 금융 식별번호'")
    private String userFinanceId;

    @Column(columnDefinition = "VARCHAR(100) NOT NULL COMMENT '이메일(계정 아이디)'")
    private String email;

    @Column(columnDefinition = "VARCHAR(255) NOT NULL COMMENT '비밀번호'")
    private String password;

    @Column(columnDefinition = "VARCHAR(20) NOT NULL COMMENT '이름'")
    private String name;

    @Column(columnDefinition = "VARCHAR(20) NOT NULL COMMENT '전화번호'")
    private String tel;

    @Column(columnDefinition = "VARCHAR(1) COMMENT '탈퇴 여부(탈퇴시, Y)'")
    private String deleted;

    @Column(columnDefinition = "DATETIME NOT NULL COMMENT '가입일자'")
    private LocalDateTime createdDate;

    @Column (columnDefinition = "DATETIME NOT NULL COMMENT '수정일자'")
    private LocalDateTime updatedDate;

    @OneToMany(mappedBy = "leader", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Group> Groups;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GroupMember> GroupMembers; // 그룹 구성원의 멤버 ID

}
