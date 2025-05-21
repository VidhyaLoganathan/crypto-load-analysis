package com.cypher.cardload.service;

import com.cypher.cardload.model.TokenTransfer;
import com.cypher.cardload.repository.TokenTransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigInteger;
import java.time.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableScheduling
@Component
@RequiredArgsConstructor
@Slf4j
public class SyncManager implements ApplicationRunner {

    private static final LocalDate START_DATE = LocalDate.of(2025, 1, 1);
    private static final ZoneId ZONE_ID = ZoneId.of("America/Los_Angeles");
    private static final int MAX_BACKOFF_SEC = 60;

    private final BlockchainService blockchainService;
    private final TokenTransferRepository transferRepo;

    /**
     * Called once on application startup: backfill from START_DATE through yesterday.
     */
    @Override
    public void run(ApplicationArguments args) {
        LocalDate today = LocalDate.now(ZONE_ID);
        LocalDate targetEnd = today.minusDays(1);
        if (!targetEnd.isBefore(START_DATE)) {
            log.info("Startup backfill from {} to {}", START_DATE, targetEnd);
            syncRange(
                    START_DATE.atStartOfDay(ZONE_ID).toInstant(),
                    targetEnd.plusDays(1).atStartOfDay(ZONE_ID).toInstant()
            );
            log.info("Startup backfill complete");
        } else {
            log.info("Nothing to backfill on startup (today = {})", today);
        }
    }

    /**
     * Scheduled to run daily at 03:30 AM America/Los_Angeles time to pick up
     * the next missing calendar day.
     */
    @Scheduled(cron = "0 30 3 * * *", zone = "America/Los_Angeles")
    public void scheduledDailySync() {
        LocalDate today = LocalDate.now(ZONE_ID);
        LocalDate lastDate = transferRepo.findTopByOrderByTimestampDesc()
                .map(TokenTransfer::getTimestamp)
                .map(ts -> ts.atZone(ZONE_ID).toLocalDate())
                .orElse(START_DATE.minusDays(1));
        LocalDate nextDate = lastDate.plusDays(1);
        if (!nextDate.isBefore(today)) {
            log.info("Up to date—lastDate={} today={}", lastDate, today);
            return;
        }
        log.info("Scheduled sync for {}", nextDate);
        syncRange(
                nextDate.atStartOfDay(ZONE_ID).toInstant(),
                nextDate.plusDays(1).atStartOfDay(ZONE_ID).toInstant()
        );
    }

    /**
     * Core sync logic: for each calendar day in [startInclusive…endExclusive),
     * skip if data already exists, otherwise fetch & persist with exponential backoff.
     */
    private void syncRange(Instant startInclusive, Instant endExclusive) {
        LocalDate start = LocalDateTime.ofInstant(startInclusive, ZoneOffset.UTC).toLocalDate();
        LocalDate end = LocalDateTime.ofInstant(endExclusive, ZoneOffset.UTC).toLocalDate();

        for (LocalDate date = start; !date.isEqual(end); date = date.plusDays(1)) {
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
            if (transferRepo.existsByTimestampBetween(dayStart, dayEnd)) {
                log.debug("Data exists for {}, skipping", date);
                continue;
            }

            Instant from = date.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant to = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            int backoff = 1;

            while (true) {
                log.info("Fetching transfers for {}", date);
                List<TokenTransfer> fetched =
                        blockchainService.getTokenTransfersToMasterWallet(from, to);
                if (!fetched.isEmpty()) {
                    transferRepo.saveAll(fetched);
                    log.info("Saved {} transfers for {}", fetched.size(), date);
                } else {
                    log.info("No transfers on {}", date);
                }
                break;
            }

            // gentle pacing between days
            sleep(2);
        }
        log.info("Sync range {} → {} complete", start, end.minusDays(1));
    }

    private void sleep(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
