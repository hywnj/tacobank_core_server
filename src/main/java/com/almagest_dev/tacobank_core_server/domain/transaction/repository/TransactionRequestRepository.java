package com.almagest_dev.tacobank_core_server.domain.transaction.repository;

import com.almagest_dev.tacobank_core_server.domain.transaction.model.TransactionRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRequestRepository extends JpaRepository<TransactionRequest, Long> {
}
