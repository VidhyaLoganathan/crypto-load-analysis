package com.cypher.cardload.service;

import com.cypher.cardload.model.CounterpartyInfo;
import com.cypher.cardload.model.CounterpartyType;
import com.cypher.cardload.dto.WalletAnalysisResponse;
import com.cypher.cardload.repository.CounterpartyRepository;
import com.cypher.cardload.util.TransactionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.Transaction;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for analyzing wallet transactions and identifying counterparties
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WalletAnalysisService {
    private final TransactionService transactionService;
    private final TransactionCacheService cacheService;
    private final CounterpartyRepository counterpartyRepository;
    private final ContractDetectionService contractDetectionService;
    private final TransactionUtils transactionUtils;

    /**
     * Analyzes a wallet to find its top counterparties
     *
     * @param walletAddress the wallet address to analyze
     * @param limit the maximum number of counterparties to return
     * @return WalletAnalysisResponse containing the analysis results
     */
    public WalletAnalysisResponse analyzeWallet(String walletAddress, int limit) {
        try {
            // Normalize wallet address to lowercase
            walletAddress = walletAddress.toLowerCase();

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

    /**
     * Counts transactions by counterparty address
     *
     * @param walletAddress the wallet address being analyzed
     * @param transactions list of transactions involving the wallet
     * @return map of counterparty addresses to transaction counts
     */
    private Map<String, Integer> countTransactionsByCounterparty(String walletAddress, List<Transaction> transactions) {
        Map<String, Integer> counterpartyCounts = new HashMap<>();

        for (Transaction tx : transactions) {
            String counterpartyAddress;

            // Determine if this wallet is sender or receiver
            if (walletAddress.equalsIgnoreCase(tx.getFrom())) {
                // This wallet is the sender, counterparty is the receiver
                counterpartyAddress = tx.getTo();

                // For token transfers, extract the actual recipient from the transaction data
                Optional<String> recipientOpt = transactionUtils.extractTokenTransferRecipient(tx);
                if (recipientOpt.isPresent()) {
                    String normalizedRecipient = recipientOpt.get().toLowerCase();
                    counterpartyCounts.put(normalizedRecipient,
                            counterpartyCounts.getOrDefault(normalizedRecipient, 0) + 1);
                }
            } else {
                // This wallet is the receiver, counterparty is the sender
                counterpartyAddress = tx.getFrom();
            }

            // Skip null addresses (can happen with contract creation transactions)
            if (counterpartyAddress == null) {
                continue;
            }

            // Normalize address to lowercase
            counterpartyAddress = counterpartyAddress.toLowerCase();

            // Increment count for this counterparty
            counterpartyCounts.put(counterpartyAddress,
                    counterpartyCounts.getOrDefault(counterpartyAddress, 0) + 1);
        }

        return counterpartyCounts;
    }

    /**
     * Gets the top counterparties by transaction count
     *
     * @param counterpartyCounts map of counterparty addresses to transaction counts
     * @param limit maximum number of counterparties to return
     * @return list of top counterparties
     */
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

    /**
     * Enriches counterparty data with metadata such as name, type, and protocol
     *
     * @param counterparties list of counterparties to enrich
     */
    private void enrichCounterpartyData(List<CounterpartyInfo> counterparties) {
        // Use parallel streams for better performance with multiple API calls
        counterparties.parallelStream().forEach(counterparty -> {
            try {
                // Normalize address
                String address = counterparty.getAddress().toLowerCase();
                counterparty.setAddress(address);

                // Add Etherscan URL for frontend linking
                counterparty.setEtherscanUrl(transactionUtils.getEtherscanUrl(address));

                // 1. Check if this is a known entity in our repository
                counterpartyRepository.findByAddress(address)
                        .ifPresent(knownEntity -> {
                            counterparty.setName(knownEntity.getName());
                            counterparty.setType(knownEntity.getType());
                            counterparty.setProtocol(knownEntity.getProtocol());
                            counterparty.setKnownEntity(true);
                        });

                // 2. If not known from repository, try to detect protocol
                if (!counterparty.isKnownEntity()) {
                    // First try to detect protocol - this also checks if it's a known address
                    String protocol = contractDetectionService.detectProtocol(address);

                    if (protocol != null && !protocol.isEmpty()) {
                        // It's a known protocol
                        counterparty.setProtocol(protocol);

                        // Set name based on protocol if not already set
                        if (counterparty.getName() == null || counterparty.getName().isEmpty()) {
                            counterparty.setName(protocol + " contract");
                        }

                        counterparty.setType(CounterpartyType.PROTOCOL);
                    } else {
                        // Not a known protocol, check if it's a contract
                        boolean isContract = contractDetectionService.isContract(address);
                        CounterpartyType type = isContract ? CounterpartyType.CONTRACT : CounterpartyType.WALLET;
                        counterparty.setType(type);

                        // Set default name based on type
                        if (isContract) {
                            counterparty.setName("contract");
                        } else {
                            counterparty.setName("wallet");
                        }
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
