
package com.cypher.cardload.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class LoadVolumeData {
    private LocalDate date;
    private BigDecimal volume;
    public LoadVolumeData(LocalDate date, BigDecimal volume) {
        this.date = date; this.volume = volume;
    }
    // getters
}
