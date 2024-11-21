package com.almagest_dev.tacobank_core_server.domain.group.repository;


import com.almagest_dev.tacobank_core_server.domain.group.model.Group;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;
import java.util.Optional;

public interface GroupRepository extends JpaRepository<Group, Long> {

    List<Group> findByLeaderId(Long leaderId);
    Optional<Group> findByName(String name);
}
