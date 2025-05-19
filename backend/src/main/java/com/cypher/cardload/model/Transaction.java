
package com.cypher.cardload.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Data
public class Transaction {
    private String hash;
    private String fromAddress;
    private String toAddress;
    private String tokenAddress;
    private BigDecimal amount;
    private LocalDateTime timestamp;
    private boolean isLoad; // Flag to indicate if this is a load transaction to the master wallet
}