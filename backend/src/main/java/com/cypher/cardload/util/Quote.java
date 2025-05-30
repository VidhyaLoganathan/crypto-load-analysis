package com.cypher.cardload.util;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class Quote {
    private final BigDecimal usdPrice;
    private final BigDecimal reserveUsd;
    private final BigDecimal volumeUsd24h;
}