package com.cypher.cardload.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.Transaction;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class TransactionCacheService {
    private final Map<String, CachedTransactions> transactionCache = new ConcurrentHashMap<>();

    // Cache expiration time in milliseconds (1 hour)
    private static final long CACHE_EXPIRATION = 3600000;

    public void cacheTransactions(String walletAddress, List<Transaction> transactions) {
        transactionCache.put(walletAddress.toLowerCase(),
                new CachedTransactions(transactions, System.currentTimeMillis()));
        log.debug("Cached {} transactions for wallet {}", transactions.size(), walletAddress);
    }

    public List<Transaction> getCachedTransactions(String walletAddress) {
        CachedTransactions cached = transactionCache.get(walletAddress.toLowerCase());

        if (cached == null) {
            return null;
        }

        // Check if cache has expired
        if (System.currentTimeMillis() - cached.timestamp > CACHE_EXPIRATION) {
            transactionCache.remove(walletAddress.toLowerCase());
            log.debug("Cache expired for wallet {}", walletAddress);
            return null;
        }

        log.debug("Returning {} cached transactions for wallet {}",
                cached.transactions.size(), walletAddress);
        return cached.transactions;
    }

    public void clearCache() {
        transactionCache.clear();
        log.info("Transaction cache cleared");
    }

    private static class CachedTransactions {
        private final List<Transaction> transactions;
        private final long timestamp;

        CachedTransactions(List<Transaction> transactions, long timestamp) {
            this.transactions = transactions;
            this.timestamp = timestamp;
        }
    }
}