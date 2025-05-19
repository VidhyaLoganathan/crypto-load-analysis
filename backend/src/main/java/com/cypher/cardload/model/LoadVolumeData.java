
package com.cypher.cardload.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoadVolumeData {
    private LocalDate date;
    private BigDecimal totalUsdValue;
    private Map<String, BigDecimal> tokenBreakdown; // token symbol -> USD value
}
