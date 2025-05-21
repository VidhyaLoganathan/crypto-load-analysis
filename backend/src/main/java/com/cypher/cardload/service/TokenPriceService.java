package com.cypher.cardload.service;

import com.cypher.cardload.config.Constants;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jService;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.http.HttpService;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class TokenPriceService {

    private final RestTemplate rest = new RestTemplate();
    private final Web3jService web3jService;
    private final Web3j web3j;

    // In-memory cache for daily ETH/USD prices
    private final Map<LocalDate, BigDecimal> ethUsdCache = new ConcurrentHashMap<>();

    public TokenPriceService() {
        // Inline a Base RPC endpoint for Web3j
        HttpService http = new HttpService("https://mainnet.base.org");
        this.web3jService = http;
        this.web3j        = Web3j.build(http);
    }

    /**
     * Bulk‐preload ETH→USD prices for each day in [from…to], using CoinGecko's market_chart/range.
     * Populates ethUsdCache so subsequent lookups are O(1).
     */
    public void preloadEthUsdPrices(LocalDate from, LocalDate to) {
        long fromTs = from.atStartOfDay(ZoneOffset.UTC).toEpochSecond();
        long   toTs = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toEpochSecond();
        String url = "https://api.coingecko.com/api/v3/coins/ethereum/market_chart/range"
                + "?vs_currency=usd"
                + "&from=" + fromTs
                + "&to="   + toTs;

        JsonNode root = rest.getForObject(url, JsonNode.class);
        for (JsonNode point : root.path("prices")) {
            long    ms    = point.get(0).asLong();
            BigDecimal price = new BigDecimal(point.get(1).asText());
            LocalDate d    = Instant.ofEpochMilli(ms)
                    .atZone(ZoneOffset.UTC)
                    .toLocalDate();
            ethUsdCache.putIfAbsent(d, price);
        }
        log.info("Preloaded ETH/USD prices for {}→{} ({} entries)", from, to, ethUsdCache.size());
    }

    /**
     * O(1) lookup of ETH→USD for the day of `timestamp`.
     * If missing, falls back to CoinGecko history endpoint (once per date), then CryptoCompare, then on‐chain.
     */
    public BigDecimal getEthUsdPrice(LocalDateTime timestamp) {
        LocalDate date = timestamp.atZone(ZoneOffset.UTC).toLocalDate();
        // Fast path: in-memory
        BigDecimal cached = ethUsdCache.get(date);
        if (cached != null) {
            return cached;
        }
        // Fallback to per-date CoinGecko
        return fetchEthUsdForDate(date)
                .or(() -> Optional.of(fetchEthUsdFromCryptoCompare(timestamp)))
                .orElseGet(() -> getOnChainEthUsdPrice(timestamp));
    }

    @Cacheable(value = "ethUsdPrices", key = "#timestamp.toLocalDate()")
    private Optional<BigDecimal> fetchEthUsdForDate(LocalDate date) {
        String dateStr = date.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        String url     = "https://api.coingecko.com/api/v3/coins/ethereum/history"
                + "?date=" + dateStr
                + "&localization=false";
        try {
            JsonNode root = rest.getForObject(url, JsonNode.class);
            BigDecimal usd = new BigDecimal(
                    root.path("market_data")
                            .path("current_price")
                            .path("usd")
                            .asText()
            );
            ethUsdCache.put(date, usd);
            log.debug("CoinGecko {} → USD = {}", dateStr, usd);
            return Optional.of(usd);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 429) {
                log.warn("CoinGecko rate limit on {}: {}", dateStr, e.getStatusCode());
            } else {
                log.warn("CoinGecko lookup failed for {}: {}", dateStr, e.getMessage());
            }
        } catch (Exception e) {
            log.warn("CoinGecko error for {}: {}", dateStr, e.getMessage());
        }
        return Optional.empty();
    }

    private BigDecimal fetchEthUsdFromCryptoCompare(LocalDateTime timestamp) {
        long unix = timestamp.toEpochSecond(ZoneOffset.UTC);
        String url = "https://min-api.cryptocompare.com/data/pricehistorical"
                + "?fsym=ETH&tsyms=USD&ts=" + unix;
        try {
            JsonNode node = rest.getForObject(url, JsonNode.class);
            BigDecimal usd = new BigDecimal(node.path("ETH").path("USD").asText());
            ethUsdCache.put(timestamp.toLocalDate(), usd);
            log.debug("CryptoCompare {} → USD = {}", unix, usd);
            return usd;
        } catch (Exception e) {
            log.warn("CryptoCompare lookup failed for {}: {}", unix, e.getMessage());
            return null;
        }
    }

    private BigDecimal getOnChainEthUsdPrice(LocalDateTime timestamp) {
        try {
            BigInteger block = findBlockByBinarySearch(timestamp);
            String pool      = Constants.AERODROME_POOL_ADDRESSES.get("USDC-ETH");
            BigInteger[] p   = getPoolPrices(pool, DefaultBlockParameter.valueOf(block));
            BigDecimal price = new BigDecimal(p[1])
                    .multiply(BigDecimal.TEN.pow(18))
                    .divide(new BigDecimal(p[0]).multiply(BigDecimal.TEN.pow(6)),
                            18, RoundingMode.HALF_UP);
            ethUsdCache.put(timestamp.toLocalDate(), price);
            return price;
        } catch (Exception e) {
            log.error("On-chain ETH price failed for {}: {}", timestamp, e.getMessage());
            return BigDecimal.valueOf(2650.00);
        }
    }

    private BigInteger findBlockByBinarySearch(LocalDateTime ts) throws IOException {
        Instant target = ts.toInstant(ZoneOffset.UTC);
        BigInteger low  = BigInteger.ZERO;
        BigInteger high = web3j.ethBlockNumber().send().getBlockNumber();
        while (low.compareTo(high) < 0) {
            BigInteger mid = low.add(high).shiftRight(1);
            EthBlock block = web3j.ethGetBlockByNumber(
                    DefaultBlockParameter.valueOf(mid), false).send();
            Instant midTs  = Instant.ofEpochSecond(block.getBlock().getTimestamp().longValue());
            if (midTs.isBefore(target)) low = mid.add(BigInteger.ONE);
            else                        high = mid;
        }
        return low;
    }

    /**
     * Generic token→USD: ETH/WETH via above; USDC/USDT =1; DAI via pool; else =1.
     */
    public BigDecimal getTokenUsdPrice(String tokenAddress, LocalDateTime timestamp) {
        String addr = tokenAddress.toLowerCase();
        if (addr.equals(Constants.TOKEN_ADDRESSES.get("ETH").toLowerCase())
                || addr.equals(Constants.TOKEN_ADDRESSES.get("WETH").toLowerCase())) {
            return getEthUsdPrice(timestamp);
        }
        if (addr.equals(Constants.TOKEN_ADDRESSES.get("USDC").toLowerCase())
                || addr.equals(Constants.TOKEN_ADDRESSES.get("USDT").toLowerCase())) {
            return BigDecimal.ONE;
        }
        if (addr.equals(Constants.TOKEN_ADDRESSES.get("DAI").toLowerCase())) {
            try {
                BigInteger block = findBlockByBinarySearch(timestamp);
                String pool      = Constants.AERODROME_POOL_ADDRESSES.get("DAI-USDC");
                BigInteger[] p   = getPoolPrices(pool, DefaultBlockParameter.valueOf(block));
                return new BigDecimal(p[1])
                        .multiply(BigDecimal.TEN.pow(18))
                        .divide(new BigDecimal(p[0]).multiply(BigDecimal.TEN.pow(6)),
                                18, RoundingMode.HALF_UP);
            } catch (Exception e) {
                log.error("DAI historical price failed: {}", e.getMessage());
            }
        }
        return BigDecimal.ONE;
    }

    private BigInteger[] getPoolPrices(String poolAddr, DefaultBlockParameter block) throws Exception {
        Function fn = new Function(
                "slot0",
                Collections.emptyList(),
                Arrays.asList(
                        new TypeReference<Uint256>() {}, // sqrtPriceX96
                        new TypeReference<Uint256>() {}, // tick
                        new TypeReference<Uint256>() {}, // obsIndex
                        new TypeReference<Uint256>() {}, // obsCard
                        new TypeReference<Uint256>() {}, // obsCardNext
                        new TypeReference<Uint256>() {}, // feeProtocol
                        new TypeReference<Uint256>() {}  // unlocked
                )
        );
        String data = FunctionEncoder.encode(fn);
        EthCall call = web3j.ethCall(
                Transaction.createEthCallTransaction(null, poolAddr, data),
                block
        ).send();
        if (call.hasError()) {
            throw new RuntimeException("slot0() failed: " + call.getError().getMessage());
        }
        List<Type> decoded           = FunctionReturnDecoder.decode(call.getValue(), fn.getOutputParameters());
        Uint256 sqrtX96Wrapped       = (Uint256) decoded.get(0);
        BigInteger sqrtX96           = sqrtX96Wrapped.getValue();
        BigDecimal sq    = new BigDecimal(sqrtX96).pow(2);
        BigDecimal two192 = new BigDecimal(BigInteger.ONE.shiftLeft(192));
        BigInteger price0 = sq.divide(two192, 0, RoundingMode.HALF_UP).toBigInteger();
        BigInteger price1 = two192.divide(sq, 0, RoundingMode.HALF_UP).toBigInteger();
        return new BigInteger[]{ price0, price1 };
    }
}
