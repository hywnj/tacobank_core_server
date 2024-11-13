package com.almagest_dev.tacobank_core_server.domain.account.repository;

import com.almagest_dev.tacobank_core_server.domain.account.model.MainAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MainAccountRepository extends JpaRepository<MainAccount, Long> {
}
