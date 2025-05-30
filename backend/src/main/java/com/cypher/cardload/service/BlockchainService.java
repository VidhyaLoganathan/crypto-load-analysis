package com.cypher.cardload.service;

import com.cypher.cardload.config.CommonPools;
import com.cypher.cardload.config.Constants;
import com.cypher.cardload.model.TokenTransfer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.web3j.abi.datatypes.Address;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class BlockchainService {

    /**
     * Known token symbols derived from CommonPools (e.g. "USDC", "WETH", etc.)
     */
    private static final Set<String> KNOWN_SYMBOLS = CommonPools.POOL_ADDRESSES.values().stream()
            .flatMap(map -> map.keySet().stream())
            .flatMap(key -> Stream.of(key.split("-")))
            .collect(Collectors.toUnmodifiableSet());

    //getting non-english symbols as part of tokenSymbol in response
    private static final Pattern NON_ASCII = Pattern.compile("[^\\p{ASCII}]");
    private final OkHttpClient httpClient;
    private final ObjectMapper mapper;
    private final TokenPriceService priceService;
    public BlockchainService(TokenPriceService priceService) {
        this.httpClient = new OkHttpClient();
        this.mapper = new ObjectMapper();
        this.priceService = priceService;
    }

    private static String asciiOnly(String s) {
        // NFKC: Compatibility decomposition, then strip anything outside ASCII
        String normalized = Normalizer.normalize(s, Normalizer.Form.NFKC);
        return NON_ASCII.matcher(normalized).replaceAll("");
    }

    /**
     * Pulls all ETH & ERC-20 transfers *to* MASTER_WALLET between fromTs and toTs.
     */
    public List<TokenTransfer> getTokenTransfersToMasterWallet(Instant fromTs, Instant toTs) {
        log.debug("getTokenTransfersToMasterWallet() → from={} to={}", fromTs, toTs);
        try {
            BigInteger startBlock = fetchBlockByTimestamp(fromTs);
            BigInteger endBlock = fetchBlockByTimestamp(toTs);
            log.debug("Resolved blocks → startBlock={} , endBlock={}", startBlock, endBlock);

            List<TokenTransfer> all = new ArrayList<>();
            all.addAll(fetchErc20Transfers(startBlock, endBlock));
            all.addAll(fetchEthTransfers(startBlock, endBlock));

            log.info("Fetched total {} transfers to master wallet", all.size());
            return all;
        } catch (Exception e) {
            log.error("Failed fetching transfers from {} to {}", fromTs, toTs, e);
            return Collections.emptyList();
        }
    }

    private BigInteger fetchBlockByTimestamp(Instant ts) throws IOException {
        log.debug("fetchBlockByTimestamp() → ts={}", ts);
        HttpUrl url = HttpUrl.parse(Constants.BASESCAN_API_URL).newBuilder()
                .addQueryParameter("module", "block")
                .addQueryParameter("action", "getblocknobytime")
                .addQueryParameter("timestamp", Long.toString(ts.getEpochSecond()))
                .addQueryParameter("closest", "before")
                .addQueryParameter("apikey", Constants.BASESCAN_API_KEY)
                .build();
        log.debug("Block API URL: {}", url);

        Request req = new Request.Builder().url(url).get().build();
        //TODO : Uncomment it so that we don't get API rate limit from basescan. But it makes things slow.
//        try {
//            Thread.sleep(5000);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
        try (Response resp = httpClient.newCall(req).execute()) {
            String body = resp.body().string();
            JsonNode root = mapper.readTree(body);
            if (!"1".equals(root.path("status").asText())) {
              //  log.warn("Block lookup returned status={} body={}", root.path("status").asText(), root);
                throw new IllegalStateException("Block lookup failed: " + root);
            }
            String blk = root.path("result").asText();
            log.info("Resolved block {} for timestamp {}", blk, ts);
            return new BigInteger(blk);
        }
    }

    private List<TokenTransfer> fetchErc20Transfers(BigInteger start, BigInteger end) throws IOException {
        log.debug("fetchErc20Transfers() → start={} end={}", start, end);
        List<TokenTransfer> out = new ArrayList<>();
        int page = 1, offset = 1000;

        while (true) {
            HttpUrl url = HttpUrl.parse(Constants.BASESCAN_API_URL).newBuilder()
                    .addQueryParameter("module", "account")
                    .addQueryParameter("action", "tokentx")
                    .addQueryParameter("address", Constants.MASTER_WALLET_ADDRESS)
                    .addQueryParameter("startblock", start.toString())
                    .addQueryParameter("endblock", end.toString())
                    .addQueryParameter("page", Integer.toString(page))
                    .addQueryParameter("offset", Integer.toString(offset))
                    .addQueryParameter("sort", "asc")
                    .addQueryParameter("apikey", Constants.BASESCAN_API_KEY)
                    .build();

            log.info("ERC20 URL: {}", url);
            Request req = new Request.Builder().url(url).get().build();
            try (Response resp = httpClient.newCall(req).execute()) {
                String respBody = resp.body().string();
                //log.info("ERC20 raw response (page {}): {}", page, respBody);

                JsonNode arr = mapper.readTree(respBody).path("result");
                if (!arr.isArray() || arr.size() == 0) {
                    log.debug("No more ERC20 transfers on page {}", page);
                    break;
                }

                for (JsonNode tx : arr) {
                    String txHash = tx.path("hash").asText();
                    MDC.put("txHash", txHash);
                    String blockNumber = tx.path("blockNumber").asText();
                    BigInteger rawAmt = new BigInteger(tx.path("value").asText());
                    int decimals = tx.path("tokenDecimal").asInt(18);
                    BigDecimal human = new BigDecimal(rawAmt)
                            .divide(BigDecimal.TEN.pow(decimals), 18, RoundingMode.HALF_UP);
                    Instant tsInst = Instant.ofEpochSecond(tx.path("timeStamp").asLong());
                    LocalDateTime ts = LocalDateTime.ofInstant(tsInst, ZoneId.systemDefault());

                    //validate tokenSymbol , since we are getting spam names
                    String rawSymbol = tx.path("tokenSymbol").asText("");
                    // strip confusables, uppercase for set lookup
                    String cleanSymbol = asciiOnly(rawSymbol).toUpperCase(Locale.ROOT);
                    if (!KNOWN_SYMBOLS.contains(cleanSymbol)) {
                        log.info("Skipping tokenSymbol : {}", cleanSymbol);
                        MDC.remove("txHash");
                        continue;
                    }

                    // Token USD value
                    BigDecimal tokenPriceUsd = priceService.getTokenUsdPrice(
                            new Address(tx.path("contractAddress").asText()), tsInst
                    );
                    BigDecimal tokenUsdVal = human.multiply(tokenPriceUsd);

                    // Fee in ETH → USD
                    BigInteger gasUsed = new BigInteger(tx.path("gasUsed").asText());
                    BigInteger gasPriceWei = new BigInteger(tx.path("gasPrice").asText());
                    BigDecimal feeEth = new BigDecimal(gasUsed)
                            .multiply(new BigDecimal(gasPriceWei))
                            .divide(BigDecimal.TEN.pow(18), 18, RoundingMode.HALF_UP);
                    BigDecimal feeUsd = feeEth.multiply(priceService.getEthUsdPrice(tsInst)).setScale(4, RoundingMode.HALF_UP);

                    BigDecimal totalUsdValue = tokenUsdVal.add(feeUsd).setScale(4, RoundingMode.HALF_UP);

                    // Log per-transaction summary
                    log.info("ERC20 tx={} block={} totalUsd=${}", txHash, blockNumber, totalUsdValue);

                    out.add(TokenTransfer.builder()
                            .txHash(txHash)
                            .tokenAddress(tx.path("contractAddress").asText())
                            .tokenSymbol(tx.path("tokenSymbol").asText())
                            .fromAddress(tx.path("from").asText())
                            .amount(rawAmt)
                            .usdValue(totalUsdValue)
                            .timestamp(ts)
                            .decimals(decimals)
                            .build()
                    );
                    MDC.remove(txHash);
                }
            }

            page++;
        }

        log.debug("Total ERC20 transfers fetched: {}", out.size());
        return out;
    }

    private List<TokenTransfer> fetchEthTransfers(BigInteger start, BigInteger end) throws IOException {
        log.debug("fetchEthTransfers() → start={} end={}", start, end);
        List<TokenTransfer> out = new ArrayList<>();
        int page = 1, offset = 1000;

        while (true) {
            HttpUrl url = HttpUrl.parse(Constants.BASESCAN_API_URL).newBuilder()
                    .addQueryParameter("module", "account")
                    .addQueryParameter("action", "txlist")
                    .addQueryParameter("address", Constants.MASTER_WALLET_ADDRESS)
                    .addQueryParameter("startblock", start.toString())
                    .addQueryParameter("endblock", end.toString())
                    .addQueryParameter("page", Integer.toString(page))
                    .addQueryParameter("offset", Integer.toString(offset))
                    .addQueryParameter("sort", "asc")
                    .addQueryParameter("apikey", Constants.BASESCAN_API_KEY)
                    .build();

            log.info("ETH URL: {}", url);
            Request req = new Request.Builder().url(url).get().build();
            try (Response resp = httpClient.newCall(req).execute()) {
                String respBody = resp.body().string();
              //  log.info("ETH raw response (page {}): {}", page, respBody);

                JsonNode arr = mapper.readTree(respBody).path("result");
                if (!arr.isArray() || arr.size() == 0) {
                    log.debug("No more ETH transfers on page {}", page);
                    break;
                }

                for (JsonNode tx : arr) {
                    //Accept only a successful transaction
                    if (tx.path("isError").asInt() != 1) {
                        String txHash = tx.path("hash").asText();
                        MDC.put("txHash", txHash);
                        String blockNumber = tx.path("blockNumber").asText();
                        BigInteger wei = new BigInteger(tx.path("value").asText());
                        BigDecimal eth = new BigDecimal(wei)
                                .divide(BigDecimal.TEN.pow(18), 18, RoundingMode.HALF_UP);
                        Instant tsInst = Instant.ofEpochSecond(tx.path("timeStamp").asLong());
                        LocalDateTime ts = LocalDateTime.ofInstant(tsInst, ZoneId.systemDefault());
                        BigDecimal ethUsdPrice = priceService.getEthUsdPrice(tsInst);
                        BigDecimal ethUsdVal = eth.multiply(ethUsdPrice);

                        // Fee
                        BigInteger gasUsed = new BigInteger(tx.path("gasUsed").asText());
                        BigInteger gasPriceWei = new BigInteger(tx.path("gasPrice").asText());
                        BigDecimal feeEth = new BigDecimal(gasUsed)
                                .multiply(new BigDecimal(gasPriceWei))
                                .divide(BigDecimal.TEN.pow(18), 18, RoundingMode.HALF_UP);
                        BigDecimal feeUsd = feeEth.multiply(ethUsdPrice).setScale(4, RoundingMode.HALF_UP);

                        BigDecimal totalUsdValue = ethUsdVal.add(feeUsd).setScale(4, RoundingMode.HALF_UP);

                        // Log per-transaction summary
                        log.info("ETH   tx={} block={} totalUsd=${}", txHash, blockNumber, totalUsdValue);

                        out.add(TokenTransfer.builder()
                                .txHash(txHash)
                                .tokenAddress(Constants.ZERO_ADDRESS)
                                .tokenSymbol("ETH")
                                .fromAddress(tx.path("from").asText())
                                .amount(wei)
                                .usdValue(totalUsdValue)
                                .timestamp(ts)
                                .decimals(18)
                                .build()
                        );
                        MDC.remove(txHash);
                    }
                }
            }

            page++;
        }

        log.debug("Total ETH transfers fetched: {}", out.size());
        return out;
    }

    /**
     * Ping BaseScan by fetching the latest block for "now".
     */
    public boolean pingBaseScan() {
        log.debug("pingBaseScan()");
        try {
            BigInteger blk = fetchBlockByTimestamp(Instant.now());
            boolean ok = (blk != null && blk.signum() > 0);
            log.info("pingBaseScan result = {}", ok);
            return ok;
        } catch (Exception e) {
            log.error("BaseScan ping failed", e);
            return false;
        }
    }
}
