package com.almagest_dev.tacobank_core_server.domain.group.repository;


import com.almagest_dev.tacobank_core_server.domain.group.model.Group;
import com.almagest_dev.tacobank_core_server.domain.member.model.Member;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;


import java.util.List;
import java.util.Optional;

public interface GroupRepository extends JpaRepository<Group, Long> {

    List<Group> findByLeaderId(Long leaderId);
    Optional<Group> findByName(String name);

    @Query("SELECT g.leader FROM Group g WHERE g.leader.id = :leaderId")
    Optional<Member> findLeaderById(@Param("leaderId") Long leaderId);


}
