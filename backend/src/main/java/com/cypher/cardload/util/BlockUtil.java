package com.cypher.cardload.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterNumber;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Timestamp → first block ≥ timestamp, with a tiny cache for speed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BlockUtil {
    private final Web3j web3j;
    private final Map<Long, BigInteger> cache = new ConcurrentHashMap<>();

    public BigInteger blockAt(Instant ts) {
        long epoch = ts.getEpochSecond();
        if (cache.containsKey(epoch)) return cache.get(epoch);
        try {
            BigInteger latest = web3j.ethBlockNumber().send().getBlockNumber();
            BigInteger low = BigInteger.ZERO, high = latest, ans = latest;
            while (low.compareTo(high) <= 0) {
                BigInteger mid = low.add(high).shiftRight(1);
                long tsMid = web3j.ethGetBlockByNumber(new DefaultBlockParameterNumber(mid), false).send().getBlock().getTimestamp().longValue();
                if (tsMid >= epoch) { ans = mid; high = mid.subtract(BigInteger.ONE);} else { low = mid.add(BigInteger.ONE);} }
            cache.put(epoch, ans);
            return ans;
        } catch (Exception e) { log.warn("blockAt lookup failed", e); return BigInteger.ZERO; }
    }
}