package com.almagest_dev.tacobank_core_server.domain.account.repository;

import com.almagest_dev.tacobank_core_server.domain.account.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;


public interface AccountRepository extends JpaRepository<Account, Long> {

}
