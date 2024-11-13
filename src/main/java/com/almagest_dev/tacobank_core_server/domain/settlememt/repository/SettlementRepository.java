package com.almagest_dev.tacobank_core_server.domain.settlememt.repository;


import com.almagest_dev.tacobank_core_server.domain.settlememt.model.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {
}
