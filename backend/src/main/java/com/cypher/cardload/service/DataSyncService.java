package com.cypher.cardload.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataSyncService {

    private final BlockchainService blockchainService;

    // Store the last processed block
    private final AtomicReference<BigInteger> lastProcessedBlock = new AtomicReference<>(null);

    // Initialize on startup
    @Scheduled(initialDelay = 1000, fixedDelay = Long.MAX_VALUE)
    public void initialize() {
        try {
            // Set the last processed block to the current block
            BigInteger currentBlock = blockchainService.getCurrentBlockNumber();
            lastProcessedBlock.set(currentBlock);
            log.info("Initialized last processed block to {}", currentBlock);
        } catch (Exception e) {
            log.error("Error initializing data sync service: ", e);
        }
    }

    // Sync new blocks every 5 minutes
//  TODO:  @Scheduled(fixedRate = 300000)
    public void syncNewBlocks() {
        try {
            BigInteger currentBlock = blockchainService.getCurrentBlockNumber();
            BigInteger lastBlock = lastProcessedBlock.get();

            if (lastBlock == null) {
                // Service not yet initialized
                return;
            }

            if (currentBlock.compareTo(lastBlock) > 0) {
                log.info("Syncing blocks from {} to {}", lastBlock.add(BigInteger.ONE), currentBlock);

                // Get transfers for the new blocks
                blockchainService.getTokenTransfersToMasterWallet(lastBlock.add(BigInteger.ONE), currentBlock);

                // Update last processed block
                lastProcessedBlock.set(currentBlock);
                log.info("Sync complete. Last processed block updated to {}", currentBlock);
            }
        } catch (Exception e) {
            log.error("Error syncing new blocks: ", e);
        }
    }
}