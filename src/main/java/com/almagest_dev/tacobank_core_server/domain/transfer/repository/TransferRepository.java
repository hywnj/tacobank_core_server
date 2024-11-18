package com.almagest_dev.tacobank_core_server.domain.transfer.repository;

import com.almagest_dev.tacobank_core_server.domain.transfer.model.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransferRepository extends JpaRepository<Transfer, Long> {

}
