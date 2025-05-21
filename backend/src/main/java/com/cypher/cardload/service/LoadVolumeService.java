package com.cypher.cardload.service;

import com.cypher.cardload.model.LoadVolumeData;
import com.cypher.cardload.model.TokenTransfer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoadVolumeService {

    private final BlockchainService blockchainService;
    private final TokenPriceService tokenPriceService;

    public List<LoadVolumeData> getDailyLoadVolume(LocalDate startDate, LocalDate endDate) {
        List<TokenTransfer> transfers = fetchTransfersForDateRange(startDate, endDate);
        // Group by date
        Map<LocalDate, List<TokenTransfer>> transfersByDate = transfers.stream()
                .collect(Collectors.groupingBy(t -> t.getTimestamp().toLocalDate()));
        // Sort and calculate
        return transfersByDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> calculateVolumeData(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    public List<LoadVolumeData> getWeeklyLoadVolume(LocalDate startDate, LocalDate endDate) {
        List<TokenTransfer> transfers = fetchTransfersForDateRange(startDate, endDate);
        WeekFields wf = WeekFields.of(Locale.getDefault());
        Map<String, List<TokenTransfer>> byWeek = transfers.stream()
                .collect(Collectors.groupingBy(t -> {
                    LocalDate d = t.getTimestamp().toLocalDate();
                    int y = d.getYear(), w = d.get(wf.weekOfWeekBasedYear());
                    return String.format("%d-W%02d", y, w);
                }));
        return byWeek.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    LocalDate weekDate = e.getValue().stream()
                            .map(t -> t.getTimestamp().toLocalDate())
                            .min(LocalDate::compareTo)
                            .orElse(startDate);
                    return calculateVolumeData(weekDate, e.getValue());
                })
                .collect(Collectors.toList());
    }

    public List<LoadVolumeData> getMonthlyLoadVolume(LocalDate startDate, LocalDate endDate) {
        List<TokenTransfer> transfers = fetchTransfersForDateRange(startDate, endDate);
        Map<String, List<TokenTransfer>> byMonth = transfers.stream()
                .collect(Collectors.groupingBy(t -> {
                    LocalDate d = t.getTimestamp().toLocalDate();
                    return String.format("%d-%02d", d.getYear(), d.getMonthValue());
                }));
        return byMonth.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    String[] parts = e.getKey().split("-");
                    LocalDate monthDate = LocalDate.of(
                            Integer.parseInt(parts[0]),
                            Integer.parseInt(parts[1]),
                            1
                    );
                    return calculateVolumeData(monthDate, e.getValue());
                })
                .collect(Collectors.toList());
    }

    public LoadVolumeData getSummary(LocalDate startDate, LocalDate endDate) {
        List<TokenTransfer> transfers = fetchTransfersForDateRange(startDate, endDate);
        return calculateVolumeData(startDate, transfers);
    }

    private List<TokenTransfer> fetchTransfersForDateRange(LocalDate startDate, LocalDate endDate) {
        try {
            // adjust to valid range
            if (startDate.getYear() < 2025) {
                startDate = LocalDate.of(2025, 1, 1);
            }
            if (LocalDate.now().isBefore(endDate)) {
                endDate = LocalDate.now();
            }

            // preload all ETH/USD rates in one go
            tokenPriceService.preloadEthUsdPrices(startDate, endDate);

            // convert to instants
            Instant from = startDate.atStartOfDay().toInstant(ZoneOffset.UTC);
            Instant to   = endDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

            return blockchainService.getTokenTransfersToMasterWallet(from, to);
        } catch (Exception e) {
            log.error("Error fetching transfers for date range {}–{}: {}",
                    startDate, endDate, e.getMessage());
            return Collections.emptyList();
        }
    }

    private LoadVolumeData calculateVolumeData(LocalDate date, List<TokenTransfer> transfers) {
        BigDecimal totalUsd = BigDecimal.ZERO;
        Map<String, BigDecimal> breakdown = new HashMap<>();

        for (TokenTransfer t : transfers) {
            BigDecimal usd = t.getUsdValue();
            totalUsd = totalUsd.add(usd);
            breakdown.merge(
                    t.getTokenSymbol(),
                    usd,
                    BigDecimal::add
            );
        }

        return LoadVolumeData.builder()
                .date(date)
                .totalUsdValue(totalUsd)
                .tokenBreakdown(breakdown)
                .build();
    }
}
