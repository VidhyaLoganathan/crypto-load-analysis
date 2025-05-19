package com.cypher.cardload.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenTransfer {
    private String txHash;
    private String tokenAddress;
    private String tokenSymbol;
    private String fromAddress;
    private BigInteger amount;
    private BigDecimal usdValue;
    private LocalDateTime timestamp;
    private Integer decimals;
}
