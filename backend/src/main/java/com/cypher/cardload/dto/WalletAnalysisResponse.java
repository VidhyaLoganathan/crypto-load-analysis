package com.cypher.cardload.dto;

import java.util.List;

import com.cypher.cardload.model.CounterpartyInfo;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WalletAnalysisResponse {
    private String walletAddress;
    private int totalTransactions;
    private List<CounterpartyInfo> topCounterparties;
}