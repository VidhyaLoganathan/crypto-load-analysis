package com.cypher.cardload.util;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.BigInteger;

@Getter
@AllArgsConstructor
public class TwapData {
    private final BigInteger sqrtPriceX96;
    private final BigDecimal reserveUsd;
    private final BigDecimal volumeUsd24h;
}
