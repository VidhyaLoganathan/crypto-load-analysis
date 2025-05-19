package com.cypher.cardload.service;
import com.cypher.cardload.exception.BlockchainServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.Transaction;

import java.io.IOException;
import java.math.BigInteger;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Service responsible for fetching transactions from the blockchain.
 * Provides multiple methods for transaction retrieval including direct blockchain queries
 * and blockchain explorer APIs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {
    private final Web3j web3j;
    private final RestTemplate restTemplate;

    @Value("${blockchain.base.explorer-api-url:}")
    private String explorerApiUrl;

    @Value("${blockchain.base.explorer-api-key:}")
    private String explorerApiKey;

    // Configurable parameters
    private static final int DEFAULT_BLOCK_RANGE = 1000;
    private static final int MAX_BLOCK_RANGE = 10000;
    private static final int THREAD_POOL_SIZE = 10;

    // Executor for parallel processing
    private final Executor executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

    /**
     * Fetches all transactions involving a specific wallet address.
     * First attempts to use the explorer API if configured, falls back to direct chain query.
     *
     * @param walletAddress The wallet address to fetch transactions for
     * @return List of transactions
     */
    public List<Transaction> getWalletTransactions(String walletAddress) {
        // First attempt to use explorer API if configured
        if (explorerApiUrl != null && !explorerApiUrl.isEmpty() && explorerApiKey != null && !explorerApiKey.isEmpty()) {
            try {
                log.info("Fetching transactions from explorer API for wallet: {}", walletAddress);
                return getTransactionsFromExplorer(walletAddress);
            } catch (Exception e) {
                log.warn("Failed to fetch transactions from explorer API, falling back to direct chain query", e);
            }
        }

        // Fallback to direct chain query
        try {
            log.info("Fetching transactions from blockchain for wallet: {}", walletAddress);
            return getTransactionsFromChainDirect(walletAddress);
        } catch (Exception e) {
            throw new BlockchainServiceException("Failed to fetch wallet transactions: " + e.getMessage(), e);
        }
    }

    /**
     * Fetches transactions using a blockchain explorer API (like Basescan/Etherscan)
     *
     * @param walletAddress The wallet address to fetch transactions for
     * @return List of transactions
     */
    private List<Transaction> getTransactionsFromExplorer(String walletAddress) {
        List<Transaction> transactions = new ArrayList<>();

        try {
            // Build the API URL - using Basescan API for Base chain
            String url = String.format("%s/api?module=account&action=txlist&address=%s&sort=desc&apikey=%s",
                    explorerApiUrl, walletAddress, explorerApiKey);

            log.debug("Calling explorer API: {}", url.replace(explorerApiKey, "API_KEY_HIDDEN"));

            // Make the API call
            String response = restTemplate.getForObject(url, String.class);

            if (response == null || response.isEmpty()) {
                log.warn("Empty response from explorer API for wallet: {}", walletAddress);
                return transactions;
            }

            // Parse the JSON response
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(response);

            // Check if the API call was successful
            String status = rootNode.path("status").asText();
            String message = rootNode.path("message").asText();

            if (!"1".equals(status)) {
                log.warn("Explorer API error: {} (status: {})", message, status);
                return transactions;
            }

            // Get the result array
            JsonNode resultsNode = rootNode.path("result");
            if (!resultsNode.isArray()) {
                log.warn("Explorer API did not return an array of transactions");
                return transactions;
            }

            // Process each transaction
            for (JsonNode txNode : resultsNode) {
                try {
                    Transaction tx = mapJsonToTransaction(txNode);
                    transactions.add(tx);
                } catch (Exception e) {
                    log.warn("Error mapping transaction: {}", e.getMessage());
                }
            }

            log.info("Retrieved {} transactions from explorer API for wallet: {}",
                    transactions.size(), walletAddress);

        } catch (Exception e) {
            log.error("Error fetching transactions from explorer: {}", e.getMessage());
            throw new BlockchainServiceException("Explorer API error: " + e.getMessage(), e);
        }

        return transactions;
    }

    /**
     * Maps a JSON node from the explorer API to a Transaction object
     *
     * @param txNode JSON node representing a transaction
     * @return Transaction object
     */
    private Transaction mapJsonToTransaction(JsonNode txNode) {
        // Create a new Transaction
        Transaction tx = new Transaction();

        // Set all the fields from the JSON
        tx.setHash(txNode.path("hash").asText());
        tx.setNonce(toHexString(txNode.path("nonce").asText()));
        tx.setBlockHash(txNode.path("blockHash").asText());
        tx.setBlockNumber(toHexString(txNode.path("blockNumber").asText()));
        tx.setTransactionIndex(toHexString(txNode.path("transactionIndex").asText()));
        tx.setFrom(txNode.path("from").asText());
        tx.setTo(txNode.path("to").asText());
        tx.setValue(toHexString(txNode.path("value").asText()));
        tx.setGasPrice(toHexString(txNode.path("gasPrice").asText()));
        tx.setGas(toHexString(txNode.path("gas").asText()));
        tx.setInput(txNode.path("input").asText());

        // Some fields may not be present in the API response
        if (txNode.has("creates")) {
            tx.setCreates(txNode.path("creates").asText());
        }

        // Add additional data if available
        if (txNode.has("r") && txNode.has("s") && txNode.has("v")) {
            tx.setR(txNode.path("r").asText());
            tx.setS(txNode.path("s").asText());
            tx.setV(txNode.path("v").asText());
        }

        return tx;
    }

    /**
     * Converts a decimal string to a hexadecimal string with '0x' prefix
     * Explorer APIs often return decimal strings, but Web3j expects hex strings
     *
     * @param decimalStr Decimal string
     * @return Hexadecimal string with '0x' prefix
     */
    private String toHexString(String decimalStr) {
        if (decimalStr == null || decimalStr.isEmpty()) {
            return "0x0";
        }

        // If it's already a hex string, return it
        if (decimalStr.startsWith("0x")) {
            return decimalStr;
        }

        try {
            // Convert decimal to hex
            BigInteger value = new BigInteger(decimalStr);
            return "0x" + value.toString(16);
        } catch (NumberFormatException e) {
            log.warn("Error converting '{}' to hex: {}", decimalStr, e.getMessage());
            return "0x0";
        }
    }

    /**
     * Fetches transactions by directly querying the blockchain
     * Note: This method is much slower and resource-intensive than using an explorer API,
     * especially for accounts with many transactions
     *
     * @param walletAddress The wallet address to fetch transactions for
     * @return List of transactions
     * @throws IOException If there's an error communicating with the blockchain
     */
    private List<Transaction> getTransactionsFromChainDirect(String walletAddress) throws IOException {
        // Get current block number
        BigInteger latestBlockNumber = web3j.ethBlockNumber().send().getBlockNumber();
        log.debug("Current block number: {}", latestBlockNumber);

        // Determine the start block for scanning
        // For performance reasons, we limit to a reasonable range of recent blocks
        BigInteger startBlock = latestBlockNumber.subtract(BigInteger.valueOf(DEFAULT_BLOCK_RANGE));
        if (startBlock.compareTo(BigInteger.ZERO) < 0) {
            startBlock = BigInteger.ZERO;
        }

        log.info("Scanning for transactions in blocks {} to {} for address {}",
                startBlock, latestBlockNumber, walletAddress);

        List<CompletableFuture<List<Transaction>>> futures = new ArrayList<>();

        // Process blocks in parallel for better performance
        for (BigInteger i = startBlock; i.compareTo(latestBlockNumber) <= 0; i = i.add(BigInteger.ONE)) {
            BigInteger blockNumber = i;
            CompletableFuture<List<Transaction>> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return getTransactionsForAddressInBlock(walletAddress, blockNumber);
                } catch (Exception e) {
                    log.error("Error processing block {}: {}", blockNumber, e.getMessage());
                    return new ArrayList<>();
                }
            }, executor);

            futures.add(future);
        }

        // Combine all results
        List<Transaction> allTransactions = futures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .collect(Collectors.toList());

        log.info("Found {} transactions for wallet {} in blocks {} to {}",
                allTransactions.size(), walletAddress, startBlock, latestBlockNumber);

        return allTransactions;
    }

    /**
     * Gets all transactions in a specific block that involve the given address
     *
     * @param walletAddress The wallet address to check
     * @param blockNumber The block number to examine
     * @return List of matching transactions
     */
    private List<Transaction> getTransactionsForAddressInBlock(String walletAddress, BigInteger blockNumber) {
        List<Transaction> result = new ArrayList<>();

        try {
            EthBlock.Block block = web3j.ethGetBlockByNumber(
                    DefaultBlockParameter.valueOf(blockNumber), true).send().getBlock();

            if (block == null || block.getTransactions() == null) {
                return result;
            }

            // Process all transactions in this block
            for (EthBlock.TransactionResult<?> txResult : block.getTransactions()) {
                if (txResult.get() instanceof EthBlock.TransactionObject) {
                    EthBlock.TransactionObject tx = (EthBlock.TransactionObject) txResult.get();

                    // Check if this transaction involves our target address
                    if ((tx.getFrom() != null && tx.getFrom().equalsIgnoreCase(walletAddress)) ||
                            (tx.getTo() != null && tx.getTo().equalsIgnoreCase(walletAddress))) {

                        // Get the transaction object directly from the TransactionObject
                        // In Web3j 4.9.8, the TransactionObject already is a Transaction or can be cast to one
                        Transaction transaction = (Transaction) txResult.get();
                        result.add(transaction);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error fetching transactions for block {}: {}", blockNumber, e.getMessage());
        }

        return result;
    }

    /**
     * Gets transactions for a wallet within a specific block range
     *
     * @param walletAddress The wallet address to fetch transactions for
     * @param startBlock The starting block number
     * @param endBlock The ending block number
     * @return List of transactions
     */
    public List<Transaction> getWalletTransactionsInRange(String walletAddress, BigInteger startBlock, BigInteger endBlock) {
        // Limit the block range to prevent excessive resource usage
        if (endBlock.subtract(startBlock).compareTo(BigInteger.valueOf(MAX_BLOCK_RANGE)) > 0) {
            throw new BlockchainServiceException("Block range too large. Maximum range is " + MAX_BLOCK_RANGE);
        }

        List<CompletableFuture<List<Transaction>>> futures = new ArrayList<>();

        // Process blocks in parallel
        for (BigInteger i = startBlock; i.compareTo(endBlock) <= 0; i = i.add(BigInteger.ONE)) {
            BigInteger blockNumber = i;
            CompletableFuture<List<Transaction>> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return getTransactionsForAddressInBlock(walletAddress, blockNumber);
                } catch (Exception e) {
                    log.error("Error processing block {}: {}", blockNumber, e.getMessage());
                    return new ArrayList<>();
                }
            }, executor);

            futures.add(future);
        }

        // Combine all results
        return futures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    /**
     * Get the most recent transaction count for a wallet
     *
     * @param walletAddress The wallet address to check
     * @return The number of transactions sent from this address
     * @throws IOException If there's an error communicating with the blockchain
     */
    public BigInteger getTransactionCount(String walletAddress) throws IOException {
        return web3j.ethGetTransactionCount(
                walletAddress, DefaultBlockParameterName.LATEST).send().getTransactionCount();
    }
}