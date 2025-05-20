package com.cypher.cardload.service;

import com.cypher.cardload.config.Constants;
import com.cypher.cardload.model.TokenTransfer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import okhttp3.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
public class BlockchainService {
    private static final String ALCHEMY_API_URL = Constants.ALCHEMY_API_URL;
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    private final TokenPriceService tokenPriceService;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public BlockchainService(TokenPriceService tokenPriceService) {
        this.tokenPriceService = tokenPriceService;
        this.httpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Fetches native ETH and ERC-20 transfers to the master wallet
     * between fromTimestamp and toTimestamp via alchemy_getAssetTransfers.
     */
    public List<TokenTransfer> getTokenTransfersToMasterWallet(Instant fromTimestamp, Instant toTimestamp) {
        log.info("Fetching token transfers for master wallet from {} to {}", fromTimestamp, toTimestamp);
        try {
            List<TokenTransfer> transfers = fetchAssetTransfers(fromTimestamp, toTimestamp);
            log.info("Fetched {} transfers total", transfers.size());
            return transfers;
        } catch (Exception e) {
            log.error("Error fetching transfers via Alchemy Enhanced API", e);
            return Collections.emptyList();
        }
    }

    private List<TokenTransfer> fetchAssetTransfers(Instant fromTimestamp, Instant toTimestamp) throws IOException {
        List<TokenTransfer> out = new ArrayList<>();
        String pageKey = null;

        do {
            ObjectNode params = objectMapper.createObjectNode();
            params.put("fromTimestamp", ISO_FORMATTER.format(fromTimestamp));
            params.put("toTimestamp", ISO_FORMATTER.format(toTimestamp));
            params.put("toAddress", Constants.MASTER_WALLET_ADDRESS);
            if (pageKey != null) params.put("pageKey", pageKey);
            ArrayNode categories = params.putArray("category");
            categories.add("external").add("erc20");

            ObjectNode rpc = objectMapper.createObjectNode();
            rpc.put("jsonrpc", "2.0");
            rpc.put("method", "alchemy_getAssetTransfers");
            ArrayNode rpcParams = rpc.putArray("params");
            rpcParams.add(params);
            rpc.put("id", 1);

            log.debug("Alchemy payload ▶ {}", rpc.toString());
            RequestBody body = RequestBody.create(rpc.toString(), MediaType.get("application/json"));
            Request request = new Request.Builder().url(ALCHEMY_API_URL).post(body).build();

            try (Response response = httpClient.newCall(request).execute()) {
                String bodyStr = response.body().string();
                log.debug("Alchemy raw response ▶ {}", bodyStr);
                JsonNode resultNode = objectMapper.readTree(bodyStr).path("result");
                JsonNode transfers = resultNode.path("transfers");

                for (JsonNode tx : transfers) {
                    String hash = tx.path("hash").asText();
                    String category = tx.path("category").asText();

                    // Determine timestamp
                    String tsText = tx.path("metadata").path("blockTimestamp").asText(null);
                    LocalDateTime ts;
                    if (tsText != null) {
                        ts = LocalDateTime.ofInstant(Instant.parse(tsText), ZoneId.systemDefault());
                    } else {
                        String blockNumHex = tx.path("blockNum").asText(null);
                        if (blockNumHex == null) {
                            log.warn("Skipping tx {}: no timestamp or blockNum", hash);
                            continue;
                        }
                        String timestampHex = fetchBlockTimestamp(blockNumHex);
                        long epochSec = new BigInteger(timestampHex.replaceFirst("^0x", ""), 16).longValue();
                        ts = LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSec), ZoneId.systemDefault());
                    }

                    String from = tx.path("from").asText("unknown");

                    if ("external".equals(category)) {
                        // Native ETH: handle hex-WEI or decimal-ETH
                        String valStr = tx.path("value").asText(null);
                        if (valStr == null) {
                            log.warn("Skipping external tx {}: missing value", hash);
                            continue;
                        }
                        BigDecimal ethAmount;
                        BigInteger amountWei;
                        if (valStr.startsWith("0x")) {
                            amountWei = new BigInteger(valStr.substring(2), 16);
                            ethAmount = new BigDecimal(amountWei).divide(BigDecimal.TEN.pow(18));
                        } else {
                            ethAmount = new BigDecimal(valStr);
                            amountWei = ethAmount.multiply(BigDecimal.TEN.pow(18)).toBigInteger();
                        }
                        BigDecimal usdValue = ethAmount.multiply(tokenPriceService.getEthUsdPrice(ts));

                        out.add(TokenTransfer.builder()
                                .txHash(hash)
                                .tokenAddress(Constants.ZERO_ADDRESS)
                                .tokenSymbol("ETH")
                                .fromAddress(from)
                                .amount(amountWei)
                                .usdValue(usdValue)
                                .timestamp(ts)
                                .decimals(18)
                                .build());

                    } else {
                        // ERC-20
                        JsonNode raw = tx.path("rawContract");
                        String valHex = raw.path("value").asText(null);
                        String decHex = raw.path("decimal").asText(null);
                        if (valHex == null || decHex == null) {
                            log.warn("Skipping ERC20 tx {}: missing rawContract fields", hash);
                            continue;
                        }
                        int decimals;
                        try {
                            decimals = new BigInteger(decHex.replaceFirst("^0x", ""), 16).intValue();
                        } catch (NumberFormatException ex) {
                            log.warn("Skipping ERC20 tx {}: invalid decimal '{}'", hash, decHex);
                            continue;
                        }
                        BigInteger amount = new BigInteger(valHex.replaceFirst("^0x", ""), 16);
                        BigDecimal human = new BigDecimal(amount).divide(BigDecimal.TEN.pow(decimals));

                        String tokenAddr = raw.path("address").asText(null);
                        if (tokenAddr == null) {
                            log.warn("Skipping ERC20 tx {}: missing token address", hash);
                            continue;
                        }
                        String symbol = tx.path("asset").asText(tokenAddr.substring(0, 6));
                        BigDecimal price = tokenPriceService.getTokenUsdPrice(tokenAddr, ts);
                        BigDecimal usdValue = human.multiply(price);

                        out.add(TokenTransfer.builder()
                                .txHash(hash)
                                .tokenAddress(tokenAddr)
                                .tokenSymbol(symbol)
                                .fromAddress(from)
                                .amount(amount)
                                .usdValue(usdValue)
                                .timestamp(ts)
                                .decimals(decimals)
                                .build());
                    }
                }

                pageKey = resultNode.path("pageKey").asText(null);
                log.info("Next pageKey: {}", pageKey);
            }
        } while (pageKey != null);

        log.info("alchemy_getAssetTransfers returned {} transfers", out.size());
        return out;
    }

    /**
     * Makes an eth_getBlockByNumber call to fetch block timestamp
     */
    private String fetchBlockTimestamp(String blockNumHex) throws IOException {
        ObjectNode rpc = objectMapper.createObjectNode();
        rpc.put("jsonrpc", "2.0");
        rpc.put("method", "eth_getBlockByNumber");
        ArrayNode params = rpc.putArray("params");
        params.add(blockNumHex);
        params.add(false);
        rpc.put("id", 1);

        RequestBody body = RequestBody.create(rpc.toString(), MediaType.get("application/json"));
        Request request = new Request.Builder().url(ALCHEMY_API_URL).post(body).build();
        try (Response response = httpClient.newCall(request).execute()) {
            String resp = response.body().string();
            JsonNode block = objectMapper.readTree(resp).path("result");
            return block.path("timestamp").asText();
        }
    }

    /**
     * For backward compatibility; not used by timestamp-based fetch.
     */
    public BigInteger getCurrentBlockNumber() throws IOException {
        ObjectNode rpc = objectMapper.createObjectNode();
        rpc.put("jsonrpc", "2.0");
        rpc.put("method",  "eth_blockNumber");
        rpc.putArray("params");
        rpc.put("id", 1);

        RequestBody body = RequestBody.create(rpc.toString(), MediaType.get("application/json"));
        Request request = new Request.Builder().url(ALCHEMY_API_URL).post(body).build();

        try (Response response = httpClient.newCall(request).execute()) {
            String result = response.body().string();
            BigInteger blockNumber = new BigInteger(
                    objectMapper.readTree(result).path("result").asText().replaceFirst("^0x", ""), 16
            );
            return blockNumber;
        }
    }

    /**
     * Approximate lookup by timestamp. Uses latest block as fallback.
     */
    public BigInteger getBlockNumberByTimestamp(long timestamp) throws IOException {
        return getCurrentBlockNumber();
    }
}
