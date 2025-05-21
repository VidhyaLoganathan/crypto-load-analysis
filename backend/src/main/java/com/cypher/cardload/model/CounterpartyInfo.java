package com.cypher.cardload.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "counterparty_info")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class CounterpartyInfo {
    @Id
    @Column(length = 42)
    private String address;

    @Column(name = "transaction_count", nullable = false)
    private int transactionCount;

    @Column(name = "name")
    private String name;

    @Column(name = "type")
    private CounterpartyType type;

    @Column(name = "protocol")
    private String protocol;

    @Column(name = "is_known_entity")
    private boolean isKnownEntity;

    @Column(name = "etherscan_url")
    private String etherscanUrl;
}