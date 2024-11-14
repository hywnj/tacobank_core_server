package com.almagest_dev.tacobank_core_server.domain.transaction.repository;

import com.almagest_dev.tacobank_core_server.domain.transaction.model.TransactionSuccess;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionSuccessRepository extends JpaRepository<TransactionSuccess, Long> {
}
