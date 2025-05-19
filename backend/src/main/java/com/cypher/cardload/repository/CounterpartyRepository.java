package com.cypher.cardload.repository;

import com.cypher.cardload.model.CounterpartyInfo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CounterpartyRepository {
    Optional<CounterpartyInfo> findByAddress(String address);
    List<CounterpartyInfo> findKnownEntities();
}
