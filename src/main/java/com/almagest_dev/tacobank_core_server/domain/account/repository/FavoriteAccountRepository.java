package com.almagest_dev.tacobank_core_server.domain.account.repository;

import com.almagest_dev.tacobank_core_server.domain.account.model.FavoriteAccount;
import com.almagest_dev.tacobank_core_server.domain.member.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteAccountRepository extends JpaRepository<FavoriteAccount, Long> {
    boolean existsByMemberAndAccountNumber(Member member, String accountNumber);
    List<FavoriteAccount> findAllByMember(Member member);
    Optional<FavoriteAccount> findByMemberAndAccountNumber(Member member, String accountNumber);
}
