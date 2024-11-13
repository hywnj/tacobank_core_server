package com.almagest_dev.tacobank_core_server.domain.account.repository;

import com.almagest_dev.tacobank_core_server.domain.account.model.FavoriteAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FavoriteAccountRepository extends JpaRepository<FavoriteAccount, Long> {
}
