package com.almagest_dev.tacobank_core_server.domain.group.repository;


import com.almagest_dev.tacobank_core_server.domain.group.model.Group;
import com.almagest_dev.tacobank_core_server.domain.group.model.GroupMember;
import com.almagest_dev.tacobank_core_server.domain.member.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    // groupId와 memberId로 GroupMember 찾기
    Optional<GroupMember> findByPayGroupIdAndMemberId(Long groupId, Long memberId);
    List<GroupMember> findByMemberIdAndStatus(Long memberId, String status);
    List<GroupMember> findByPayGroupAndStatus(Group payGroup, String status);
}
