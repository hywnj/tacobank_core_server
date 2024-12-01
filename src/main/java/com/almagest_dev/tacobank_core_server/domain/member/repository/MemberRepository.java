package com.almagest_dev.tacobank_core_server.domain.member.repository;


import com.almagest_dev.tacobank_core_server.domain.member.model.Member;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    @Query("SELECT m FROM Member m WHERE m.id = :id AND m.deleted = :deleted")
    Optional<Member> findByIdAndDeleted(@Param("id") Long id, @Param("deleted") String deleted);

    Optional<Member> findByTel(String tel);

    Optional<Member> findByEmailAndTel(String email, String tel);
    Optional<Member> findByEmail(String email);

    Optional<Member> findMemberById(Long id);
}
