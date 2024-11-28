package com.almagest_dev.tacobank_core_server.domain.bankCode;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrgCodeRepository extends JpaRepository<OrgCode, Long> {
    OrgCode findByCode(String code);
}
