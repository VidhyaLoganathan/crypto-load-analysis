package com.cypher.cardload.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class WalletAnalysisResponse {
    private String walletAddress;
    private int totalTransactions;
    private List<CounterpartyInfo> topCounterparties;
}