package com.cypher.cardload.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "token_transfers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenTransfer {

    /**
     * We use the tx hash + logIndex (if ERC-20) or hash+“0” (for ETH)
     * as the primary key to ensure uniqueness even if the same tx
     * has multiple events.
     */
    @Id
    @Column(name = "tx_hash", length = 100, nullable = false)
    private String txHash;

    @Column(name = "token_address", length = 42, nullable = false)
    private String tokenAddress;

    @Column(name = "token_symbol", length = 50, nullable = false)
    private String tokenSymbol;

    @Column(name = "from_address", length = 42, nullable = false)
    private String fromAddress;

    @Column(name = "amount", precision = 38, scale = 0, nullable = false)
    private java.math.BigInteger amount;

    @Column(name = "usd_value", precision = 30, scale = 10, nullable = false)
    private BigDecimal usdValue;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "decimals", nullable = false)
    private Integer decimals;
}
