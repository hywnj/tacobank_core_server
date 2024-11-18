package com.almagest_dev.tacobank_core_server.domain.account.repository;

import com.almagest_dev.tacobank_core_server.domain.account.model.Account;
import com.almagest_dev.tacobank_core_server.domain.member.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByIdAndVerificated(Long id, String verificated);
    long countByMember(Member member);
    Optional<Account> findByIdAndMember(Long id, Member member);
    Optional<Account> findByAccountNumber(String accountNumber);
}
