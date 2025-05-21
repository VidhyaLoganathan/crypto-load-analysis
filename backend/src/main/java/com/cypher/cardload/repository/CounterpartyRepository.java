package com.cypher.cardload.repository;

import com.cypher.cardload.model.CounterpartyInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CounterpartyRepository
        extends JpaRepository<CounterpartyInfo, String> {
    // You get: findById, findAll, save, delete, etc.

    // If you still need to fetch only known entities:
    List<CounterpartyInfo> findByIsKnownEntityTrue();
    Optional<CounterpartyInfo> findByAddress(String address);
}
