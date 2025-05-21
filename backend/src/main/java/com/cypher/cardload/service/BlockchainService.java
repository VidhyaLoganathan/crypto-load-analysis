package com.cypher.cardload.service;

import com.cypher.cardload.config.Constants;
import com.cypher.cardload.model.TokenTransfer;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.http.HttpService;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BlockchainService {
    private static final String    ALCHEMY_API_URL = Constants.ALCHEMY_API_URL;
    private static final MediaType JSON_TYPE       = MediaType.get("application/json");

    private final TokenPriceService tokenPriceService;
    private final OkHttpClient      httpClient;
    private final ObjectMapper      objectMapper;
    private final Web3j             web3j;
    private final ExecutorService   executor;

    public BlockchainService(TokenPriceService tokenPriceService) {
        this.tokenPriceService = tokenPriceService;
        this.httpClient        = new OkHttpClient();
        this.objectMapper      = new ObjectMapper();
        this.web3j             = Web3j.build(new HttpService(ALCHEMY_API_URL));

        int threads = Math.max(2, Runtime.getRuntime().availableProcessors());
        this.executor = Executors.newFixedThreadPool(threads);
    }

    public List<TokenTransfer> getTokenTransfersToMasterWallet(Instant fromTs, Instant toTs) {
        try {
            BigInteger fromBlock = getBlockNumberByTimestamp(fromTs);
            BigInteger toBlock   = getBlockNumberByTimestamp(toTs);
            log.info("Parallel fetch: block range {} → {}", fromBlock, toBlock);

            int chunks = ((ThreadPoolExecutor) executor).getCorePoolSize();
            BigInteger total = toBlock.subtract(fromBlock).add(BigInteger.ONE);
            BigInteger chunkSize = total
                    .add(BigInteger.valueOf(chunks).subtract(BigInteger.ONE))
                    .divide(BigInteger.valueOf(chunks));

            List<CompletableFuture<List<TokenTransfer>>> futures = new ArrayList<>();
            BigInteger start = fromBlock;
            for (int i = 0; i < chunks && start.compareTo(toBlock) <= 0; i++) {
                BigInteger end = start.add(chunkSize).subtract(BigInteger.ONE);
                if (end.compareTo(toBlock) > 0) end = toBlock;
                BigInteger s = start, e = end;
                futures.add(CompletableFuture.supplyAsync(() -> {
                    try {
//                        Thread.sleep(500);
                        return fetchAssetTransfersChunk(s, e);
                    } catch (IOException ex) {
                        log.error("Chunk {}–{} failed", s, e, ex);
                        return Collections.emptyList();
                    }
//                    catch (InterruptedException ex) {
//                        throw new RuntimeException(ex);
//                    }
                }, executor));
                start = end.add(BigInteger.ONE);
            }

            List<TokenTransfer> all = futures.stream()
                    .map(CompletableFuture::join)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
            log.info("Total transfers fetched: {}", all.size());
            return all;
        } catch (Exception ex) {
            log.error("Parallel fetch failed", ex);
            return Collections.emptyList();
        }
    }

    private List<TokenTransfer> fetchAssetTransfersChunk(BigInteger fromBlock,
                                                         BigInteger toBlock) throws IOException {
        List<TokenTransfer> out = new ArrayList<>();
        String pageKey = null;
        do {
            ObjectNode params = objectMapper.createObjectNode();
            params.put("fromBlock",    "0x" + fromBlock.toString(16));
            params.put("toBlock",      "0x" + toBlock.toString(16));
            params.put("toAddress",    Constants.MASTER_WALLET_ADDRESS);
            params.put("withMetadata", true);
            params.put("maxCount",     "0x3e8");
            if (pageKey != null) params.put("pageKey", pageKey);
            ArrayNode cats = params.putArray("category");
            cats.add("external").add("erc20");

            ObjectNode rpc = objectMapper.createObjectNode();
            rpc.put("jsonrpc", "2.0");
            rpc.put("method",  "alchemy_getAssetTransfers");
            rpc.put("id",      1);
            rpc.putArray("params").add(params);

            RequestBody body = RequestBody.create(rpc.toString(), JSON_TYPE);
            Request request  = new Request.Builder().url(ALCHEMY_API_URL).post(body).build();
            try (Response resp = httpClient.newCall(request).execute()) {
                String json = resp.body().string();
                JsonNode result = objectMapper.readTree(json).path("result");
                JsonNode txs    = result.path("transfers");
                for (JsonNode tx : txs) {
                    TokenTransfer t = parseTransfer(tx);
                    if (t != null) out.add(t);
                }
                pageKey = result.path("pageKey").asText(null);
            }
        } while (pageKey != null);
        return out;
    }

    private TokenTransfer parseTransfer(JsonNode tx) {
        try {
            String hash     = tx.path("hash").asText();
            String category = tx.path("category").asText();
            // Timestamp
            String tsText = tx.path("metadata").path("blockTimestamp").asText(null);
            LocalDateTime ts;
            if (tsText != null) {
                ts = LocalDateTime.ofInstant(Instant.parse(tsText), ZoneId.systemDefault());
            } else {
                String blkHex  = tx.path("blockNum").asText(null);
                if (blkHex == null || blkHex.equals("null")) return null;
                String timeHex = fetchBlockTimestamp(blkHex);
                long epoch = new BigInteger(timeHex.replaceFirst("^0x", ""), 16).longValue();
                ts = LocalDateTime.ofInstant(Instant.ofEpochSecond(epoch), ZoneId.systemDefault());
            }
            String from = tx.path("from").asText("unknown");

            if ("external".equals(category)) {
                String val = tx.path("value").asText(null);
                if (val == null) return null;
                BigInteger wei = val.startsWith("0x")
                        ? new BigInteger(val.substring(2), 16)
                        : new BigInteger(new BigDecimal(val).multiply(BigDecimal.TEN.pow(18)).toBigInteger().toString());
                BigDecimal eth = new BigDecimal(wei).divide(BigDecimal.TEN.pow(18));
                BigDecimal usd = eth.multiply(tokenPriceService.getEthUsdPrice(ts));
                return TokenTransfer.builder()
                        .txHash(hash).tokenAddress(Constants.ZERO_ADDRESS).tokenSymbol("ETH")
                        .fromAddress(from).amount(wei).usdValue(usd)
                        .timestamp(ts).decimals(18).build();
            } else {
                JsonNode raw = tx.path("rawContract");
                String valHex = raw.path("value").asText(null);
                String decHex = raw.path("decimal").asText(null);
                if (valHex == null || decHex == null) return null;
                BigInteger amount = new BigInteger(valHex.replaceFirst("^0x", ""), 16);
                int decimals = new BigInteger(decHex.replaceFirst("^0x", ""), 16).intValue();
                BigDecimal human = new BigDecimal(amount).divide(BigDecimal.TEN.pow(decimals));
                String tokenAddr = raw.path("address").asText(null);
                if (tokenAddr == null) return null;
                String symbol = tx.path("asset").asText(tokenAddr.substring(0, 6));
                BigDecimal price = tokenPriceService.getTokenUsdPrice(tokenAddr, ts);
                return TokenTransfer.builder()
                        .txHash(hash).tokenAddress(tokenAddr).tokenSymbol(symbol)
                        .fromAddress(from).amount(amount).usdValue(human.multiply(price))
                        .timestamp(ts).decimals(decimals).build();
            }
        } catch (Exception e) {
            log.warn("Skipping transfer due to parse error", e);
            return null;
        }
    }

    private String fetchBlockTimestamp(String blockNumHex) throws IOException {
        ObjectNode rpc = objectMapper.createObjectNode();
        rpc.put("jsonrpc", "2.0");
        rpc.put("method",  "eth_getBlockByNumber");
        rpc.put("id",      1);
        ArrayNode params = rpc.putArray("params");
        params.add(blockNumHex).add(false);
        RequestBody body = RequestBody.create(rpc.toString(), JSON_TYPE);
        Request request = new Request.Builder().url(ALCHEMY_API_URL).post(body).build();
        try (Response response = httpClient.newCall(request).execute()) {
            String resp = response.body().string();
            JsonNode block = objectMapper.readTree(resp).path("result");
            return block.path("timestamp").asText();
        }
    }

    private BigInteger getBlockNumberByTimestamp(Instant ts) {
        long unix = ts.getEpochSecond();
        try {
            ObjectNode rpc = objectMapper.createObjectNode();
            rpc.put("jsonrpc", "2.0");
            rpc.put("method",  "alchemy_getBlockByTimestamp");
            rpc.put("id",      1);
            rpc.putArray("params").add(unix).add("before");
            RequestBody body = RequestBody.create(rpc.toString(), JSON_TYPE);
            Request req = new Request.Builder().url(ALCHEMY_API_URL).post(body).build();
            try (Response resp = httpClient.newCall(req).execute()) {
                String hex = objectMapper.readTree(resp.body().string()).path("result").asText();
                return new BigInteger(hex.replaceFirst("^0x", ""), 16);
            }
        } catch (Exception e) {
            log.warn("Proprietary RPC failed: {}; using binary search", e.getMessage());
            return binarySearchBlock(ts);
        }
    }

    private BigInteger binarySearchBlock(Instant target) {
        try {
            BigInteger low  = BigInteger.ZERO;
            BigInteger high = web3j.ethBlockNumber().send().getBlockNumber();
            while (low.compareTo(high) < 0) {
                BigInteger mid = low.add(high).shiftRight(1);
                EthBlock b = web3j.ethGetBlockByNumber(DefaultBlockParameter.valueOf(mid), false).send();
                Instant midTs = Instant.ofEpochSecond(b.getBlock().getTimestamp().longValue());
                if (midTs.isBefore(target)) low = mid.add(BigInteger.ONE);
                else                        high = mid;
            }
            return low;
        } catch (Exception ex) {
            log.error("Binary search failed; defaulting to latest", ex);
            try {
                return web3j.ethBlockNumber().send().getBlockNumber();
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
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
}
