package com.cypher.cardload.service;

import com.cypher.cardload.config.Constants;
import com.cypher.cardload.model.LoadVolumeData;
import com.cypher.cardload.model.TokenTransfer;
import com.cypher.cardload.repository.TokenTransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoadVolumeService {

    private final BlockchainService blockchainService;
    private final TokenTransferRepository transferRepo;

    /**
     * Ensure every calendar day in [startDate…endDate] is backed by DB data.
     * For any missing day, fetch its transfers & persist.
     */
    private List<TokenTransfer> loadOrFetchPerDay(LocalDate startDate, LocalDate endDate) {
        Instant startInstant = startDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endInstant   = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        // 1) load existing
        List<TokenTransfer> existing = transferRepo.findByTimestampBetween(
                LocalDateTime.ofInstant(startInstant, ZoneOffset.UTC),
                LocalDateTime.ofInstant(endInstant,   ZoneOffset.UTC)
        );

        // 2) build set of all dates
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        Set<LocalDate> allDates = Stream.iterate(startDate, d -> d.plusDays(1))
                .limit(days)
                .collect(Collectors.toSet());

        // 3) find covered dates
        Set<LocalDate> covered = existing.stream()
                .map(t -> t.getTimestamp().toLocalDate())
                .collect(Collectors.toSet());

        // 4) for each missing date, fetch & persist
        for (LocalDate d : allDates) {
            if (!covered.contains(d)) {
                Instant from = d.atStartOfDay(ZoneOffset.UTC).toInstant();
                Instant to   = d.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
                log.info("No DB data for {}; fetching transfers…", d);
                List<TokenTransfer> fetched =
                        blockchainService.getTokenTransfersToMasterWallet(from, to);

                if (!fetched.isEmpty()) {
                    transferRepo.saveAll(fetched);
                    existing.addAll(fetched);
                    log.info("Fetched & saved {} transfers for {}", fetched.size(), d);
                } else {
                    log.info("No transfers on {}", d);
                }
            }
        }
        return existing;
    }

    public List<LoadVolumeData> getDailyLoadVolume(LocalDate startDate, LocalDate endDate) {
        List<TokenTransfer> all = loadOrFetchPerDay(startDate, endDate);

        return all.stream()
                .collect(Collectors.groupingBy(t -> t.getTimestamp().toLocalDate()))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> calculateVolumeData(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    public List<LoadVolumeData> getWeeklyLoadVolume(LocalDate startDate, LocalDate endDate) {
        List<TokenTransfer> all = loadOrFetchPerDay(startDate, endDate);

        WeekFields wf = WeekFields.of(Locale.getDefault());
        Map<LocalDate, List<TokenTransfer>> byWeek = all.stream()
                .collect(Collectors.groupingBy(t -> {
                    LocalDate d = t.getTimestamp().toLocalDate();
                    int y = d.getYear(), w = d.get(wf.weekOfWeekBasedYear());
                    // represent week by its Monday
                    return LocalDate.ofYearDay(y, 1)
                            .with(wf.weekBasedYear(), y)
                            .with(wf.weekOfWeekBasedYear(), w)
                            .with(wf.dayOfWeek(), 1);
                }));

        return byWeek.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> calculateVolumeData(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    public List<LoadVolumeData> getMonthlyLoadVolume(LocalDate startDate, LocalDate endDate) {
        List<TokenTransfer> all = loadOrFetchPerDay(startDate, endDate);

        Map<YearMonth, List<TokenTransfer>> byMonth = all.stream()
                .collect(Collectors.groupingBy(t ->
                        YearMonth.from(t.getTimestamp().toLocalDate())
                ));

        return byMonth.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    LocalDate monthStart = e.getKey().atDay(1);
                    return calculateVolumeData(monthStart, e.getValue());
                })
                .collect(Collectors.toList());
    }

    public LoadVolumeData getSummary(LocalDate startDate, LocalDate endDate) {
        List<TokenTransfer> all = loadOrFetchPerDay(startDate, endDate);
        return calculateVolumeData(startDate, all);
    }

    private LoadVolumeData calculateVolumeData(LocalDate date, List<TokenTransfer> transfers) {
        BigDecimal totalUsd = BigDecimal.ZERO;
        Map<String, BigDecimal> breakdown = new HashMap<>();

        for (TokenTransfer t : transfers) {
            BigDecimal usd = t.getUsdValue();
            totalUsd = totalUsd.add(usd);
            breakdown.merge(t.getTokenSymbol(), usd, BigDecimal::add);
        }

        return LoadVolumeData.builder()
                .date(date)
                .totalUsdValue(totalUsd)
                .tokenBreakdown(breakdown)
                .build();
    }
}
