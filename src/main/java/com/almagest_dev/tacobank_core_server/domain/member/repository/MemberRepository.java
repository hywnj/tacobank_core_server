package com.almagest_dev.tacobank_core_server.domain.member.repository;


import com.almagest_dev.tacobank_core_server.domain.member.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByIdAndDeleted(Long id, String deleted); // 탈퇴하지 않은 멤버만 조회

    Optional<Member> findByTel(String tel);

    Optional<Member> findByEmailAndTel(String email, String tel);
}
