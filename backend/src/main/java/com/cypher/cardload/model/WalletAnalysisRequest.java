package com.cypher.cardload.model;

import lombok.Data;

@Data
public class WalletAnalysisRequest {
    private String walletAddress;
    private int limit = 10; // Default to top 10
}
