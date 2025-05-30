package com.cypher.cardload.service;

import com.cypher.cardload.config.Constants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Off-chain price service using CryptoCompare and CoinGecko APIs
 * to fetch USD prices when on-chain quotes are unavailable.
 * Supports both current and historical prices by timestamp.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OffChainService {
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .callTimeout(5, TimeUnit.SECONDS)
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    // CoinGecko requires coin IDs, not symbols
    private static final Map<String,String> COINGECKO_IDS = Map.ofEntries(
            Map.entry("ETH", "ethereum"),
            Map.entry("USDC", "usd-coin"),
            Map.entry("USDT", "tether"),
            Map.entry("DAI",  "dai"),
            Map.entry("WBTC", "wrapped-bitcoin"),
            Map.entry("LINK", "chainlink"),
            Map.entry("AAVE", "aave"),
            Map.entry("FRAX", "frax"),
            Map.entry("FXS",  "frax-share"),
            Map.entry("MAI",  "mai"),
            Map.entry("OP",   "optimism")
    );

    private static final DateTimeFormatter CG_DATE_FMT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy").withZone(ZoneId.of("UTC"));

    /**
     * Fetch current USD price for symbol.
     */
    public Optional<BigDecimal> getPrice(String symbol) {
        try {
            Optional<BigDecimal> cc = fetchFromCryptoCompare(symbol);
            if (cc.isPresent()) return cc;
            Optional<BigDecimal> cg = fetchFromCoinGecko(symbol);
            return cg;
        } catch (Exception e) {
            log.warn("Current price fetch failed for {}: {}", symbol, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Fetch historical USD price for symbol at given timestamp.
     */
    public Optional<BigDecimal> getPrice(String symbol, Instant timestamp) {
        try {
            Optional<BigDecimal> cc = fetchHistoricalFromCryptoCompare(symbol, timestamp);
            if (cc.isPresent()) return cc;
            Optional<BigDecimal> cg = fetchHistoricalFromCoinGecko(symbol, timestamp);
            if (cg.isPresent()) return cg;
            // fallback to current price
            return getPrice(symbol);
        } catch (Exception e) {
            log.warn("Historical price fetch failed for {} at {}: {}",
                    symbol, timestamp, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<BigDecimal> fetchFromCryptoCompare(String symbol) throws IOException {
        HttpUrl url = HttpUrl.parse(Constants.CRYPTOCOMPARE_API_URL).newBuilder()
                .addQueryParameter("fsym", symbol)
                .addQueryParameter("tsyms", "USD")
                .addQueryParameter("api_key", Constants.CRYPTOCOMPARE_API_KEY)
                .build();
        Request req = new Request.Builder().url(url).get().build();
        try (Response resp = httpClient.newCall(req).execute()) {
            if (!resp.isSuccessful()) return Optional.empty();
            JsonNode root = mapper.readTree(resp.body().string());
            if (root.has("USD")) {
                return Optional.of(new BigDecimal(root.get("USD").asText()));
            }
            return Optional.empty();
        }
    }

    private Optional<BigDecimal> fetchHistoricalFromCryptoCompare(
            String symbol, Instant ts) throws IOException {
        HttpUrl url = HttpUrl.parse(Constants.CRYPTOCOMPARE_HISTORICAL_URL).newBuilder()
                .addQueryParameter("fsym", symbol)
                .addQueryParameter("tsyms", "USD")
                .addQueryParameter("ts", Long.toString(ts.getEpochSecond()))
                .addQueryParameter("api_key", Constants.CRYPTOCOMPARE_API_KEY)
                .build();
        Request req = new Request.Builder().url(url).get().build();
        try (Response resp = httpClient.newCall(req).execute()) {
            if (!resp.isSuccessful()) return Optional.empty();
            JsonNode root = mapper.readTree(resp.body().string());
            JsonNode data = root.path(symbol.toUpperCase());
            if (data.has("USD")) {
                return Optional.of(new BigDecimal(data.get("USD").asText()));
            }
            return Optional.empty();
        }
    }

    private Optional<BigDecimal> fetchFromCoinGecko(String symbol) throws IOException {
        String id = COINGECKO_IDS.get(symbol.toUpperCase());
        if (id == null) return Optional.empty();
        HttpUrl url = HttpUrl.parse(Constants.COINGECKO_API_URL)
                .newBuilder().addPathSegment("simple").addPathSegment("price")
                .addQueryParameter("ids", id)
                .addQueryParameter("vs_currencies", "usd")
                .build();
        Request req = new Request.Builder().url(url).get().build();
        try (Response resp = httpClient.newCall(req).execute()) {
            if (!resp.isSuccessful()) return Optional.empty();
            JsonNode root = mapper.readTree(resp.body().string());
            JsonNode node = root.path(id).path("usd");
            if (!node.isMissingNode()) {
                return Optional.of(new BigDecimal(node.asText()));
            }
            return Optional.empty();
        }
    }

    private Optional<BigDecimal> fetchHistoricalFromCoinGecko(
            String symbol, Instant ts) throws IOException {
        String id = COINGECKO_IDS.get(symbol.toUpperCase());
        if (id == null) return Optional.empty();
        String date = CG_DATE_FMT.format(ts);
        HttpUrl url = HttpUrl.parse(Constants.COINGECKO_API_URL)
                .newBuilder().addPathSegment("coins").addPathSegment(id)
                .addPathSegment("history")
                .addQueryParameter("date", date)
                .build();
        Request req = new Request.Builder().url(url).get().build();
        try (Response resp = httpClient.newCall(req).execute()) {
            if (!resp.isSuccessful()) return Optional.empty();
            JsonNode root = mapper.readTree(resp.body().string());
            JsonNode priceNode = root.path("market_data").path("current_price").path("usd");
            if (!priceNode.isMissingNode()) {
                return Optional.of(new BigDecimal(priceNode.asText()));
            }
            return Optional.empty();
        }
    }
}
