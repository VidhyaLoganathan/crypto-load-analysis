package com.cypher.cardload.sync;

import com.cypher.cardload.model.TokenTransfer;
import com.cypher.cardload.repository.TokenTransferRepository;
import com.cypher.cardload.service.BlockchainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
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
     * On startup:
     * 1) backfill all *whole* months from START_DATE up to last full month
     * 2) backfill the remaining days of the current month
     */
    @Override
    public void run(ApplicationArguments args) {
        LocalDate today = LocalDate.now(ZONE_ID);
        YearMonth lastFullMon = YearMonth.from(today.minusMonths(1));

        // 1) Sync all whole months from START_DATE → lastFullMon
        syncMonths(START_DATE, lastFullMon);

        // 2) Sync remaining days of the lastFullMon → today-1 day
        LocalDate daysStart = lastFullMon.atEndOfMonth().plusDays(1);
        LocalDate daysEnd = today.minusDays(1);
        if (!daysStart.isAfter(daysEnd)) {
            log.info("Startup backfill (daily) from {} to {}", daysStart, daysEnd);
            syncRange(
                    daysStart.atStartOfDay(ZONE_ID).toInstant(),
                    daysEnd.plusDays(1).atStartOfDay(ZONE_ID).toInstant()
            );
        }
        log.info("Skipping syn for local test");
    }

    /**
     * Scheduled at 04:00 AM on the 1st of each month to backfill exactly
     * the next missing calendar month.
     */
    @Scheduled(cron = "0 0 4 1 * *", zone = "America/Los_Angeles")
    public void scheduledMonthlySync() {
        // identical to your previous monthly method
        LocalDate today = LocalDate.now(ZONE_ID);
        YearMonth lastMonthInDb = transferRepo.findTopByOrderByTimestampDesc()
                .map(TokenTransfer::getTimestamp)
                .map(ts -> ts.atZone(ZONE_ID).toLocalDate().withDayOfMonth(1))
                .map(YearMonth::from)
                .orElse(YearMonth.from(START_DATE).minusMonths(1));

        YearMonth next = lastMonthInDb.plusMonths(1);
        YearMonth upTo = YearMonth.from(today.minusMonths(1));
        if (next.isAfter(upTo)) {
            log.info("Monthly sync: up to date (lastMonth={} upTo={})", lastMonthInDb, upTo);
            return;
        }

        log.info("Scheduled monthly sync: {}", next);
        syncMonths(next.atDay(1), upTo);
    }

    /**
     * Backfill every month in [fromMonth…toMonth], skipping ones already in DB.
     */
    private void syncMonths(LocalDate fromMonthStart, YearMonth toMonth) {
        YearMonth startMon = YearMonth.from(fromMonthStart);
        YearMonth endMon = toMonth;

        for (YearMonth mon = startMon; !mon.isAfter(endMon); mon = mon.plusMonths(1)) {
            LocalDate monthStart = mon.atDay(1);
            LocalDate monthEnd = mon.atEndOfMonth();

            // if any day in that month is missing, fetch the whole month
            LocalDateTime mStart = monthStart.atStartOfDay();
            LocalDateTime mEnd = monthEnd.plusDays(1).atStartOfDay();
            boolean exists = transferRepo.existsByTimestampBetween(mStart, mEnd);
            if (exists) {
                log.debug("syncMonths: skipping {}, data exists", mon);
                continue;
            }

            log.info("syncMonths: fetching whole month {}", mon);
            syncRange(
                    monthStart.atStartOfDay(ZONE_ID).toInstant(),
                    monthEnd.plusDays(1).atStartOfDay(ZONE_ID).toInstant()
            );
        }
    }

    /**
     * Fallback per-day sync for any arbitrary range [start…end).
     */
    private void syncRange(Instant startInclusive, Instant endExclusive) {
        LocalDate start = LocalDateTime.ofInstant(startInclusive, ZoneOffset.UTC).toLocalDate();
        LocalDate end = LocalDateTime.ofInstant(endExclusive, ZoneOffset.UTC).toLocalDate();

        for (LocalDate date = start; date.isBefore(end); date = date.plusDays(1)) {
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
            if (transferRepo.existsByTimestampBetween(dayStart, dayEnd)) {
                log.debug("syncRange: skipping {}, exists", date);
            } else {
                fetchAndPersistDay(date);
            }
            sleep(1);
        }
    }

    private void fetchAndPersistDay(LocalDate date) {
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
    }

    private void sleep(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
