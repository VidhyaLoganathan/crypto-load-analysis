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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoadVolumeService {

    private final BlockchainService blockchainService;

    public List<LoadVolumeData> getDailyLoadVolume(LocalDate startDate, LocalDate endDate) {
        List<TokenTransfer> transfers = fetchTransfersForDateRange(startDate, endDate);

        // Group by date
        Map<LocalDate, List<TokenTransfer>> transfersByDate = transfers.stream()
                .collect(Collectors.groupingBy(transfer -> transfer.getTimestamp().toLocalDate()));

        // Sort the map by date
        Map<LocalDate, List<TokenTransfer>> sortedTransfersByDate = new TreeMap<>(transfersByDate);

        List<LoadVolumeData> result = new ArrayList<>();

        // For each date, calculate total USD value and token breakdown
        for (Map.Entry<LocalDate, List<TokenTransfer>> entry : sortedTransfersByDate.entrySet()) {
            LoadVolumeData volumeData = calculateVolumeData(entry.getKey(), entry.getValue());
            result.add(volumeData);
        }

        return result;
    }

    public List<LoadVolumeData> getWeeklyLoadVolume(LocalDate startDate, LocalDate endDate) {
        List<TokenTransfer> transfers = fetchTransfersForDateRange(startDate, endDate);

        // Group by week
        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        Map<String, List<TokenTransfer>> transfersByWeek = transfers.stream()
                .collect(Collectors.groupingBy(transfer -> {
                    LocalDate date = transfer.getTimestamp().toLocalDate();
                    int year = date.getYear();
                    int week = date.get(weekFields.weekOfWeekBasedYear());
                    return year + "-W" + String.format("%02d", week);
                }));

        // Sort the map by week
        Map<String, List<TokenTransfer>> sortedTransfersByWeek = new TreeMap<>(transfersByWeek);

        List<LoadVolumeData> result = new ArrayList<>();

        // For each week, calculate total USD value and token breakdown
        for (Map.Entry<String, List<TokenTransfer>> entry : sortedTransfersByWeek.entrySet()) {
            String weekKey = entry.getKey();
            List<TokenTransfer> weekTransfers = entry.getValue();

            // Get the first day of this week for the date field
            LocalDate weekDate = weekTransfers.stream()
                    .map(transfer -> transfer.getTimestamp().toLocalDate())
                    .min(LocalDate::compareTo)
                    .orElse(startDate);

            // Calculate volume data
            LoadVolumeData volumeData = calculateVolumeData(weekDate, weekTransfers);
            result.add(volumeData);
        }

        return result;
    }

    public List<LoadVolumeData> getMonthlyLoadVolume(LocalDate startDate, LocalDate endDate) {
        List<TokenTransfer> transfers = fetchTransfersForDateRange(startDate, endDate);

        // Group by month
        Map<String, List<TokenTransfer>> transfersByMonth = transfers.stream()
                .collect(Collectors.groupingBy(transfer -> {
                    LocalDate date = transfer.getTimestamp().toLocalDate();
                    int year = date.getYear();
                    int month = date.getMonthValue();
                    return year + "-" + String.format("%02d", month);
                }));

        // Sort the map by month
        Map<String, List<TokenTransfer>> sortedTransfersByMonth = new TreeMap<>(transfersByMonth);

        List<LoadVolumeData> result = new ArrayList<>();

        // For each month, calculate total USD value and token breakdown
        for (Map.Entry<String, List<TokenTransfer>> entry : sortedTransfersByMonth.entrySet()) {
            String monthKey = entry.getKey();
            List<TokenTransfer> monthTransfers = entry.getValue();

            // Parse year and month
            int year = Integer.parseInt(monthKey.split("-")[0]);
            int month = Integer.parseInt(monthKey.split("-")[1]);

            // Create date as first day of month
            LocalDate monthDate = LocalDate.of(year, month, 1);

            // Calculate volume data
            LoadVolumeData volumeData = calculateVolumeData(monthDate, monthTransfers);
            result.add(volumeData);
        }

        return result;
    }

    public LoadVolumeData getSummary(LocalDate startDate, LocalDate endDate) {
        List<TokenTransfer> transfers = fetchTransfersForDateRange(startDate, endDate);

        // Calculate summary for the entire period
        return calculateVolumeData(startDate, transfers);
    }

    // Helper method to fetch transfers for a date range
    private List<TokenTransfer> fetchTransfersForDateRange(LocalDate startDate, LocalDate endDate) {
        try {
            // Convert dates to epoch seconds
            long startEpoch = startDate.atStartOfDay().toEpochSecond(ZoneOffset.UTC);
            long endEpoch = endDate.plusDays(1).atStartOfDay().toEpochSecond(ZoneOffset.UTC);

            // Get approximate block numbers for the date range
            // In a production app, we'd use a more precise method or an indexer API
            // Only include transfers from 2025 (per requirements)
            if (startDate.getYear() < 2025) {
                startDate = LocalDate.of(2025, 1, 1);
                startEpoch = startDate.atStartOfDay().toEpochSecond(ZoneOffset.UTC);
            }

            if (LocalDate.now().isBefore(endDate)) {
                endDate = LocalDate.now();
                endEpoch = endDate.plusDays(1).atStartOfDay().toEpochSecond(ZoneOffset.UTC);
            }

            try {
//                // Get block numbers from timestamps
//                var startBlock = blockchainService.getBlockNumberByTimestamp(startEpoch);
//                var endBlock = blockchainService.getBlockNumberByTimestamp(endEpoch);
                //                return blockchainService.getTokenTransfersToMasterWallet(startBlock, endBlock);


                Instant fromInstant = Instant.ofEpochSecond(startEpoch);
                Instant toInstant   = Instant.ofEpochSecond(endEpoch);
                return blockchainService.getTokenTransfersToMasterWallet(fromInstant, toInstant);

            } catch (Exception e) {
                log.error("Error getting block numbers: {}", e.getMessage());
                return new ArrayList<>();
            }

        } catch (Exception e) {
            log.error("Error fetching transfers for date range: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    // Helper method to calculate volume data from a list of transfers
    private LoadVolumeData calculateVolumeData(LocalDate date, List<TokenTransfer> transfers) {
        BigDecimal totalUsdValue = BigDecimal.ZERO;
        Map<String, BigDecimal> tokenBreakdown = new HashMap<>();

        // Calculate total USD value and token breakdown
        for (TokenTransfer transfer : transfers) {
            BigDecimal usdValue = transfer.getUsdValue();
            totalUsdValue = totalUsdValue.add(usdValue);

            // Update token breakdown
            String tokenSymbol = transfer.getTokenSymbol();
            tokenBreakdown.put(tokenSymbol, tokenBreakdown.getOrDefault(tokenSymbol, BigDecimal.ZERO).add(usdValue));
        }

        return LoadVolumeData.builder()
                .date(date)
                .totalUsdValue(totalUsdValue)
                .tokenBreakdown(tokenBreakdown)
                .build();
    }
}
