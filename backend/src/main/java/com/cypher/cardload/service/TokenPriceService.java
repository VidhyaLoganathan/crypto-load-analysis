package com.cypher.cardload.service;

import com.cypher.cardload.util.BlockUtil;
import com.cypher.cardload.util.TwapData;
import com.cypher.cardload.util.Quote;
import com.cypher.cardload.util.TokenDayKey;
import com.cypher.cardload.util.BlockchainConstants;
import com.cypher.cardload.service.OffChainService;
import com.cypher.cardload.util.ClPoolResolver;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.abi.datatypes.Address;
import org.web3j.protocol.Web3j;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Service for retrieving USD price of tokens at given timestamps.
 * <p>
 * Implements a multi-tiered pricing strategy:
 * <ol>
 *   <li>On-chain direct stable pools</li>
 *   <li>On-chain token/WETH pools combined with ETH/USD</li>
 *   <li>Off-chain API fallback</li>
 * </ol>
 * Caches per-token-per-day prices and ETH/USD prices for efficiency.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenPriceService {

    /**
     * List of known stable tokens whose USD price is always 1.
     */
    public static final List<Address> KNOWN_TOKENS = getKnownTokenAddresses();

    /**
     * Canonical WETH token address on Base network.
     */
    private static final Address WETH = new Address("0x4200000000000000000000000000000000000006");

    private final Web3j web3j;
    private final ClPoolResolver poolResolver;
    private final BlockUtil blockUtil;
    private final OffChainService offChainService;

    /**
     * Cache mapping (token, UTC day) to computed USD price to avoid repeated lookups.
     */
    private final Cache<TokenDayKey, BigDecimal> priceCache = Caffeine.newBuilder()
            .expireAfterWrite(24, TimeUnit.HOURS)
            .maximumSize(100_000)
            .build();

    /**
     * Cache of ETH to USD prices by UTC day.
     */
    private final Map<LocalDate, BigDecimal> ethUsdCache = new ConcurrentHashMap<>();

    /**
     * Minimum required on-chain USD reserve to consider a pool quote valid.
     */
    @Value("${pricing.minReserveUsd:50000}")
    private BigDecimal minReserveUsd;

    /**
     * Minimum required 24h USD volume to consider a pool quote valid.
     */
    @Value("${pricing.minVolumeUsd:5000}")
    private BigDecimal minVolumeUsd;

    /**
     * Builds the list of known token addresses from configuration.
     *
     * @return list of token addresses considered as USD-pegged
     */
    public static List<Address> getKnownTokenAddresses() {
        Map<String, String> map = BlockchainConstants.createTokenAddressesMap();
        return map.keySet().stream()
                .map(Address::new)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves the USD price for a given token at a specific timestamp.
     * <p>
     * First checks stable tokens, then on-chain pools, and finally off-chain APIs.
     * Caches result per UTC day.
     *
     * @param token token address to price
     * @param ts    timestamp of price lookup
     * @return USD price as BigDecimal
     */
    public BigDecimal getTokenUsdPrice(Address token, Instant ts) {
        log.debug("getTokenUsdPrice() → token={} at {}", token, ts);
        if (KNOWN_TOKENS.contains(token)) {
            log.debug(" Token {} is a known USD quote. Returning 1.", token);
            return BigDecimal.ONE;
        }

        LocalDate day = ts.atZone(ZoneOffset.UTC).toLocalDate();
        TokenDayKey key = new TokenDayKey(token, day);
        return priceCache.get(key, k -> {
            log.debug("  Cache miss for {} on {} – computing price.", token, day);
            return computePrice(token, ts);
        });
    }

    /**
     * Computes the price by trying direct stable pools, then via WETH, then off-chain.
     *
     * @param token token address
     * @param ts    timestamp for lookup
     * @return computed USD price
     */
    private BigDecimal computePrice(Address token, Instant ts) {
        log.debug("computePrice() → token={} at {}", token, ts);

        // 1) direct USD stable pools
        Optional<BigDecimal> direct = quoteViaStable(token, ts);
        if (direct.isPresent()) {
            log.debug("  Direct stable quote found: {}", direct.get());
            return direct.get();
        }
        log.debug("  No direct stable quote found.");

        // 2) via ETH pools
        Optional<BigDecimal> viaEth = quoteViaEth(token, ts);
        if (viaEth.isPresent()) {
            log.debug("  Quote via ETH found: {}", viaEth.get());
            return viaEth.get();
        }
        log.debug("  No viable ETH quote found.");

        // 3) off-chain API fallback
        Optional<BigDecimal> offChainPrice = offChainService.getPrice(poolResolver.symbolOf(token));
        if (offChainPrice.isPresent()) {
            log.debug("  Off-chain price service returned: {}", offChainPrice.get());
            return offChainPrice.get();
        }

        log.warn("  All chain methods failed for {} at {}. Falling back to 1.", token, ts);
        return BigDecimal.ONE;
    }

    /**
     * Attempts to quote against known USD-pegged tokens via on-chain pools.
     *
     * @param token token address
     * @param ts    timestamp for lookup
     * @return Optional USD price if a valid pool is found
     */
    private Optional<BigDecimal> quoteViaStable(Address token, Instant ts) {
        log.debug("quoteViaStable() → token={} at {}", token, ts);
        Optional<Quote> best = KNOWN_TOKENS.stream()
                .map(q -> buildQuote(token, q, ts))
                .flatMap(Optional::stream)
                .filter(this::passesGate)
                .max(Comparator.comparing(Quote::getReserveUsd));

        if (best.isPresent()) {
            BigDecimal price = best.get().getUsdPrice();
            log.debug("  Best stable quote: {} (reserveUsd={})", price, best.get().getReserveUsd());
            return Optional.of(price);
        } else {
            log.debug("  No stable quote pools passed quality gates.");
            return Optional.empty();
        }
    }

    /**
     * Attempts to quote via a token/WETH pool and multiplies by ETH/USD.
     *
     * @param token token address
     * @param ts    timestamp for lookup
     * @return Optional USD price if valid
     */
    private Optional<BigDecimal> quoteViaEth(Address token, Instant ts) {
        log.debug("quoteViaEth() → token={} at {}", token, ts);
        Optional<Quote> q = buildQuote(token, WETH, ts).filter(this::passesGate);
        if (q.isEmpty()) {
            log.debug("  No token/WETH pool passed quality gates.");
            return Optional.empty();
        }

        BigDecimal ethUsd = getEthUsdPrice(ts);
        log.debug("  On-chain ETH/USD price: {}", ethUsd);
        if (ethUsd.signum() == 0) {
            log.warn("  ETH/USD price is zero at {}. Cannot compute via ETH.", ts);
            return Optional.empty();
        }

        BigDecimal usdPrice = q.get().getUsdPrice().multiply(ethUsd);
        log.debug("  Computed {} USD per token via ETH route.", usdPrice);
        return Optional.of(usdPrice);
    }

    /**
     * Retrieves ETH/USD price at a given timestamp, caching per day.
     *
     * @param ts timestamp for lookup
     * @return USD price of ETH
     */
    public BigDecimal getEthUsdPrice(Instant ts) {
        log.debug("getEthUsdPrice() → at {}", ts);
        LocalDate day = ts.atZone(ZoneOffset.UTC).toLocalDate();
        return ethUsdCache.computeIfAbsent(day, d -> {
            log.debug("  ETH/USD cache miss on {} – computing.", d);
            return computeEthUsdPrice(ts);
        });
    }

    /**
     * Computes ETH/USD price by selecting the best on-chain pool or falling back.
     *
     * @param ts timestamp for lookup
     * @return computed USD price of ETH
     */
    private BigDecimal computeEthUsdPrice(Instant ts) {
        log.debug("computeEthUsdPrice() → at {}", ts);
        Optional<BigDecimal> onChain = KNOWN_TOKENS.stream()
                .map(q -> buildQuote(WETH, q, ts))
                .flatMap(Optional::stream)
                .filter(this::passesGate)
                .max(Comparator.comparing(Quote::getReserveUsd))
                .map(Quote::getUsdPrice);

        if (onChain.isPresent()) {
            log.debug("  On-chain ETH/USD found: {}", onChain.get());
            return onChain.get();
        }

        log.warn("  No on-chain ETH/USD pools passed gates. Fallback to zero.");
        return offChainService.getPrice("ETH", ts).orElse(BigDecimal.ZERO);
    }

    /**
     * Builds a price quote for a token/quote pair at a given timestamp by finding the best pool,
     * invoking observeTwap, and calculating USD price and metrics.
     *
     * @param token token address
     * @param quote quote token address (e.g. stable or WETH)
     * @param ts    timestamp for lookup
     * @return Optional Quote including price, reserveUsd, and volumeUsd24h
     */
    private Optional<Quote> buildQuote(Address token, Address quote, Instant ts) {
        log.debug("buildQuote() → token={} vs {} at {}", token, quote, ts);
        Optional<Address> poolOpt = poolResolver.findBestPool(token, quote);
        if (poolOpt.isEmpty()) {
            log.debug("  No pool found for {} vs {}", token, quote);
            return Optional.empty();
        }
        Address pool = poolOpt.get();
        log.debug("  Selected pool {} for {} vs {}", pool, token, quote);

        try {
            BigInteger block = blockUtil.blockAt(ts);
            log.debug("  Resolved block {} for timestamp {}", block, ts);

            TwapData twap = poolResolver.observeTwap(pool, 900, block);
            BigDecimal priceTokPerQuote = poolResolver.sqrtPriceX96ToPrice(twap.getSqrtPriceX96(), token, quote);
            BigDecimal usdPrice = KNOWN_TOKENS.contains(quote)
                    ? priceTokPerQuote
                    : priceTokPerQuote.multiply(getEthUsdPrice(ts));

            log.debug("  TWAP price: {} per quote; reserveUsd={} ; volumeUsd24h={}",
                    usdPrice, twap.getReserveUsd(), twap.getVolumeUsd24h());

            return Optional.of(new Quote(usdPrice, twap.getReserveUsd(), twap.getVolumeUsd24h()));
        } catch (Exception e) {
            log.debug("  Exception building quote for pool {}: {}", pool.getValue(), e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Applies quality gates to ensure a quote has sufficient liquidity and activity.
     *
     * @param q Quote object containing reserve and volume metrics
     * @return true if quote meets minimum USD reserve and volume thresholds
     */
    private boolean passesGate(Quote q) {
        boolean pass = q.getReserveUsd().compareTo(minReserveUsd) >= 0
                && q.getVolumeUsd24h().compareTo(minVolumeUsd) >= 0;
        log.debug("passesGate() → reserveUsd={} (min {}) , volumeUsd24h={} (min {}) => {}",
                q.getReserveUsd(), minReserveUsd, q.getVolumeUsd24h(), minVolumeUsd, pass);
        return pass;
    }
}
