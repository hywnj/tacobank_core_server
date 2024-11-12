package com.almagest_dev.tacobank_core_server.domain.group.repository;


import com.almagest_dev.tacobank_core_server.domain.group.model.Group;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface GroupRepository extends JpaRepository<Group, Long> {
    @Query("SELECT g FROM Group g LEFT JOIN g.payGroups gm WHERE g.leader.id = :memberId OR gm.member.id = :memberId")
    List<Group> findByLeaderIdOrMemberId(@Param("memberId") Long memberId);
}
