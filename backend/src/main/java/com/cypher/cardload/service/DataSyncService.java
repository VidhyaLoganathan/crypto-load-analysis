package com.cypher.cardload.service;

import com.cypher.cardload.model.TokenTransfer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataSyncService {

    private final BlockchainService blockchainService;

    // Store the last processed timestamp
    private final AtomicReference<Instant> lastProcessedTimestamp = new AtomicReference<>(null);

    // Initialize on startup
    @Scheduled(initialDelay = 1000, fixedDelay = Long.MAX_VALUE)
    public void initialize() {
        try {
            Instant now = Instant.now();
            lastProcessedTimestamp.set(now);
            log.info("Initialized last processed timestamp to {}", now);
        } catch (Exception e) {
            log.error("Error initializing data sync service: ", e);
        }
    }

    // Sync new data every 5 minutes
//    @Scheduled(fixedRate = 300000)
    public void syncNewData() {
        try {
            Instant now = Instant.now();
            Instant last = lastProcessedTimestamp.get();

            if (last == null) {
                // Service not yet initialized
                return;
            }

            if (now.isAfter(last)) {
                log.info("Syncing transfers from {} to {}", last, now);
                List<TokenTransfer> transfers = blockchainService
                        .getTokenTransfersToMasterWallet(last, now);
                log.info("Synced {} transfers", transfers.size());

                // Update last processed timestamp
                lastProcessedTimestamp.set(now);
                log.info("Sync complete. Last processed timestamp updated to {}", now);
            }
        } catch (Exception e) {
            log.error("Error syncing data: ", e);
        }
    }
}