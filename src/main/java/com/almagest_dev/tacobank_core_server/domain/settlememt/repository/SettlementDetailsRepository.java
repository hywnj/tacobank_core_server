package com.almagest_dev.tacobank_core_server.domain.settlememt.repository;
import com.almagest_dev.tacobank_core_server.domain.group.model.GroupMember;
import com.almagest_dev.tacobank_core_server.domain.settlememt.model.SettlementDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SettlementDetailsRepository extends JpaRepository<SettlementDetails, Long> {

}
