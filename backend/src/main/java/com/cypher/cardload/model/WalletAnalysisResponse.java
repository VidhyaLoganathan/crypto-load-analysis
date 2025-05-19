package com.cypher.cardload.model;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WalletAnalysisResponse {
    private String walletAddress;
    private int totalTransactions;
    private List<CounterpartyInfo> topCounterparties;
}