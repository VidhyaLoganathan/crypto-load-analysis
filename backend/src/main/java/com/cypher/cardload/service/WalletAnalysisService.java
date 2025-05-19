package com.cypher.cardload.service;

import com.cypher.cardload.model.CounterpartyInfo;
import com.cypher.cardload.model.CounterpartyType;
import com.cypher.cardload.model.WalletAnalysisResponse;
import com.cypher.cardload.repository.CounterpartyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.Transaction;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletAnalysisService {
    private final TransactionService transactionService;
    private final TransactionCacheService cacheService;
    private final CounterpartyRepository counterpartyRepository;
    private final ContractDetectionService contractDetectionService;

    public WalletAnalysisResponse analyzeWallet(String walletAddress, int limit) {
        try {
            // Validate wallet address format
            if (!walletAddress.startsWith("0x") || walletAddress.length() != 42) {
                throw new IllegalArgumentException("Invalid wallet address format");
            }

            // Get transactions, first checking cache
            List<Transaction> transactions = cacheService.getCachedTransactions(walletAddress);
            if (transactions == null) {
                // Cache miss, fetch from blockchain
                transactions = transactionService.getWalletTransactions(walletAddress);
                // Store in cache for future requests
                cacheService.cacheTransactions(walletAddress, transactions);
            }

            // Count transactions by counterparty
            Map<String, Integer> counterpartyCounts = countTransactionsByCounterparty(walletAddress, transactions);

            // Get top counterparties
            List<CounterpartyInfo> topCounterparties = getTopCounterparties(counterpartyCounts, limit);

            // Enrich counterparty data with metadata
            enrichCounterpartyData(topCounterparties);

            return WalletAnalysisResponse.builder()
                    .walletAddress(walletAddress)
                    .totalTransactions(transactions.size())
                    .topCounterparties(topCounterparties)
                    .build();

        } catch (Exception e) {
            log.error("Error analyzing wallet {}: {}", walletAddress, e.getMessage(), e);
            throw new RuntimeException("Failed to analyze wallet: " + e.getMessage(), e);
        }
    }

    private Map<String, Integer> countTransactionsByCounterparty(String walletAddress, List<Transaction> transactions) {
        Map<String, Integer> counterpartyCounts = new HashMap<>();

        for (Transaction tx : transactions) {
            String counterpartyAddress;

            // Determine if this wallet is sender or receiver
            if (walletAddress.equalsIgnoreCase(tx.getFrom())) {
                // This wallet is the sender, counterparty is the receiver
                counterpartyAddress = tx.getTo();
            } else {
                // This wallet is the receiver, counterparty is the sender
                counterpartyAddress = tx.getFrom();
            }

            // Skip null addresses (can happen with contract creation transactions)
            if (counterpartyAddress == null) {
                continue;
            }

            // Increment count for this counterparty
            counterpartyCounts.put(counterpartyAddress,
                    counterpartyCounts.getOrDefault(counterpartyAddress, 0) + 1);
        }

        return counterpartyCounts;
    }

    private List<CounterpartyInfo> getTopCounterparties(Map<String, Integer> counterpartyCounts, int limit) {
        return counterpartyCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> CounterpartyInfo.builder()
                        .address(entry.getKey())
                        .transactionCount(entry.getValue())
                        .build())
                .collect(Collectors.toList());
    }

    private void enrichCounterpartyData(List<CounterpartyInfo> counterparties) {
        // Use parallel streams for better performance with multiple API calls
        counterparties.parallelStream().forEach(counterparty -> {
            try {
                // 1. Check if this is a known entity in our repository
                counterpartyRepository.findByAddress(counterparty.getAddress())
                        .ifPresent(knownEntity -> {
                            counterparty.setName(knownEntity.getName());
                            counterparty.setType(knownEntity.getType());
                            counterparty.setProtocol(knownEntity.getProtocol());
                            counterparty.setKnownEntity(true);
                        });

                // 2. If not known, determine if it's a contract
                if (!counterparty.isKnownEntity()) {
                    boolean isContract = contractDetectionService.isContract(counterparty.getAddress());
                    CounterpartyType type = isContract ? CounterpartyType.CONTRACT : CounterpartyType.WALLET;
                    counterparty.setType(type);

                    // 3. Try to identify protocol if it's a contract
                    if (isContract) {
                        String protocol = contractDetectionService.detectProtocol(counterparty.getAddress());
                        if (protocol != null && !protocol.isEmpty()) {
                            counterparty.setProtocol(protocol);
                            counterparty.setName("Unknown " + protocol + " contract");
                        } else {
                            counterparty.setName("Unknown contract");
                        }
                    } else {
                        counterparty.setName("Unknown wallet");
                    }
                }
            } catch (Exception e) {
                log.warn("Error enriching counterparty data for {}: {}",
                        counterparty.getAddress(), e.getMessage());
                // Set default values if enrichment fails
                if (counterparty.getType() == null) {
                    counterparty.setType(CounterpartyType.WALLET);
                }
                if (counterparty.getName() == null) {
                    counterparty.setName("Unknown");
                }
            }
        });
    }
}