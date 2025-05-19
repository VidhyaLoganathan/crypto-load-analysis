package com.cypher.cardload.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class CounterpartyInfo {
    private String address;
    private int transactionCount;
    private String name;
    private CounterpartyType type;
    private String protocol;
    private boolean isKnownEntity;
    private String etherscanUrl; // Added for frontend convenience
}
