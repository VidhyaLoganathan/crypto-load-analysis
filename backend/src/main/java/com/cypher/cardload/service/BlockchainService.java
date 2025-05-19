package com.cypher.cardload.service;

import com.cypher.cardload.config.Constants;
import com.cypher.cardload.model.TokenTransfer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.utils.Convert;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlockchainService {

    private final Web3j web3j;
    private final TokenPriceService tokenPriceService;

    // Method to get all token transfers to the master wallet
    public List<TokenTransfer> getTokenTransfersToMasterWallet(BigInteger startBlock, BigInteger endBlock) {
        List<TokenTransfer> allTransfers = new ArrayList<>();

        try {
            // First get native ETH transfers
            List<TokenTransfer> ethTransfers = getEthTransfers(startBlock, endBlock);
            allTransfers.addAll(ethTransfers);

            // Then get ERC-20 token transfers
            List<TokenTransfer> erc20Transfers = getErc20Transfers(startBlock, endBlock);
            allTransfers.addAll(erc20Transfers);

        } catch (Exception e) {
            log.error("Error fetching transfers: ", e);
        }

        return allTransfers;
    }

    private List<TokenTransfer> getEthTransfers(BigInteger startBlock, BigInteger endBlock) {
        List<TokenTransfer> transfers = new ArrayList<>();

        try {
            // Get transactions where the master wallet is the recipient
            org.web3j.protocol.core.methods.request.EthFilter filter = new org.web3j.protocol.core.methods.request.EthFilter(
                    org.web3j.protocol.core.DefaultBlockParameter.valueOf(startBlock),
                    org.web3j.protocol.core.DefaultBlockParameter.valueOf(endBlock),
                    Collections.singletonList(Constants.MASTER_WALLET_ADDRESS)
            );

            EthLog ethLog = web3j.ethGetLogs(filter).send();

            if (ethLog.hasError()) {
                log.error("Error getting ETH logs: {}", ethLog.getError().getMessage());
                return transfers;
            }

            for (EthLog.LogResult logResult : ethLog.getLogs()) {
                EthLog.LogObject log = (EthLog.LogObject) logResult;
                Transaction tx = web3j.ethGetTransactionByHash(log.getTransactionHash()).send().getTransaction().orElse(null);

                if (tx != null && Constants.MASTER_WALLET_ADDRESS.equalsIgnoreCase(tx.getTo()) && tx.getValue() != null && tx.getValue().compareTo(BigInteger.ZERO) > 0) {
                    EthBlock.Block block = web3j.ethGetBlockByHash(tx.getBlockHash(), false).send().getBlock();
                    LocalDateTime timestamp = Instant.ofEpochSecond(block.getTimestamp().longValue()).atZone(ZoneId.systemDefault()).toLocalDateTime();

                    BigDecimal ethPrice = tokenPriceService.getEthUsdPrice(timestamp);
                    BigDecimal ethAmount = Convert.fromWei(new BigDecimal(tx.getValue()), Convert.Unit.ETHER);
                    BigDecimal usdValue = ethAmount.multiply(ethPrice);

                    TokenTransfer transfer = TokenTransfer.builder()
                            .txHash(tx.getHash())
                            .tokenAddress("0x0000000000000000000000000000000000000000") // Native ETH
                            .tokenSymbol("ETH")
                            .fromAddress(tx.getFrom())
                            .amount(tx.getValue())
                            .usdValue(usdValue)
                            .timestamp(timestamp)
                            .decimals(18) // ETH has 18 decimals
                            .build();

                    transfers.add(transfer);
                }
            }
        } catch (Exception e) {
            log.error("Error fetching ETH transfers: ", e);
        }

        return transfers;
    }

    private List<TokenTransfer> getErc20Transfers(BigInteger startBlock, BigInteger endBlock) {
        List<TokenTransfer> transfers = new ArrayList<>();

        // ERC-20 Transfer event topic
        String transferEventTopic = "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef";

        try {
            // Create filter for Transfer events to master wallet
            org.web3j.protocol.core.methods.request.EthFilter filter = new org.web3j.protocol.core.methods.request.EthFilter(
                    org.web3j.protocol.core.DefaultBlockParameter.valueOf(startBlock),
                    org.web3j.protocol.core.DefaultBlockParameter.valueOf(endBlock),
                    (List<String>) null
            );

            // Filter for Transfer event
            filter.addSingleTopic(transferEventTopic);
            // We don't filter by fromAddress (second topic)
            filter.addNullTopic();
            // Filter by toAddress (third topic) - padded to 32 bytes
            filter.addSingleTopic("0x000000000000000000000000" + Constants.MASTER_WALLET_ADDRESS.substring(2));

            EthLog ethLog = web3j.ethGetLogs(filter).send();

            if (ethLog.hasError()) {
                log.error("Error getting ERC-20 logs: {}", ethLog.getError().getMessage());
                return transfers;
            }

            for (EthLog.LogResult logResult : ethLog.getLogs()) {
                EthLog.LogObject log = (EthLog.LogObject) logResult;

                // Get block timestamp
                EthBlock.Block block = web3j.ethGetBlockByHash(log.getBlockHash(), false).send().getBlock();
                LocalDateTime timestamp = Instant.ofEpochSecond(block.getTimestamp().longValue()).atZone(ZoneId.systemDefault()).toLocalDateTime();

                // Extract token address
                String tokenAddress = log.getAddress();

                // Get token symbol and decimals
                String tokenSymbol = getTokenSymbolByAddress(tokenAddress);
                int decimals = getTokenDecimals(tokenAddress);

                // Extract transfer amount from data field (last 32 bytes)
                BigInteger amount = new BigInteger(log.getData().substring(2), 16);

                // Convert to human-readable amount
                BigDecimal humanAmount = new BigDecimal(amount).divide(BigDecimal.TEN.pow(decimals));

                // Get USD value
                BigDecimal usdPrice = tokenPriceService.getTokenUsdPrice(tokenAddress, timestamp);
                BigDecimal usdValue = humanAmount.multiply(usdPrice);

                // Extract sender address from the second topic (32 bytes)
                String fromAddressTopic = log.getTopics().get(1);
                String fromAddress = "0x" + fromAddressTopic.substring(26); // Remove padding

                TokenTransfer transfer = TokenTransfer.builder()
                        .txHash(log.getTransactionHash())
                        .tokenAddress(tokenAddress)
                        .tokenSymbol(tokenSymbol)
                        .fromAddress(fromAddress)
                        .amount(amount)
                        .usdValue(usdValue)
                        .timestamp(timestamp)
                        .decimals(decimals)
                        .build();

                transfers.add(transfer);
            }
        } catch (Exception e) {
            log.error("Error fetching ERC-20 transfers: ", e);
        }

        return transfers;
    }

    // Helper method to get token symbol by address
    private String getTokenSymbolByAddress(String tokenAddress) {
        // First check our known tokens map
        for (var entry : Constants.TOKEN_ADDRESSES.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(tokenAddress)) {
                return entry.getKey();
            }
        }

        // If not found, return the first 6 chars of the address as the symbol
        return tokenAddress.substring(0, 6);
    }

    // Helper method to get token decimals
    private int getTokenDecimals(String tokenAddress) {
        // Most ERC-20 tokens use 18 decimals by default
        // In a production app, we would query the contract

        // Default for most tokens is 18
        int defaultDecimals = 18;

        // Special cases
        if (Constants.TOKEN_ADDRESSES.get("USDC").equalsIgnoreCase(tokenAddress) ||
                Constants.TOKEN_ADDRESSES.get("USDT").equalsIgnoreCase(tokenAddress)) {
            return 6; // USDC and USDT typically have 6 decimals
        }

        return defaultDecimals;
    }

    // Get current block number
    public BigInteger getCurrentBlockNumber() throws IOException {
        return web3j.ethBlockNumber().send().getBlockNumber();
    }

    // Get block by timestamp (approximate)
    public BigInteger getBlockNumberByTimestamp(long timestamp) throws IOException, ExecutionException, InterruptedException {
        // This is a simplified approach - in production, use binary search
        BigInteger latestBlock = web3j.ethBlockNumber().send().getBlockNumber();
        EthBlock.Block latestBlockDetails = web3j.ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false)
                .send().getBlock();

        long latestTimestamp = latestBlockDetails.getTimestamp().longValue();

        // Average block time on Base is about 2 seconds
        long timeDiff = latestTimestamp - timestamp;
        long blockDiff = timeDiff / 2;

        return latestBlock.subtract(BigInteger.valueOf(blockDiff));
    }
}