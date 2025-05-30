package com.cypher.cardload.util;

import com.cypher.cardload.config.CommonPools;
import com.google.common.primitives.Bytes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Int24;
import org.web3j.abi.datatypes.generated.Int256;
import org.web3j.abi.datatypes.generated.Uint128;
import org.web3j.abi.datatypes.generated.Uint160;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint32;
import org.web3j.crypto.ContractUtils;
import org.web3j.crypto.Hash;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthGetCode;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves concentrated-liquidity pools (Uniswap-v3 family) and computes true TWAPs
 * by calling observe(...) and using TickMath.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClPoolResolver {
    private final Web3j web3j;

    private static final List<Address> FACTORIES = List.of(
            new Address("0x33128a8fC17869897dcE68Ed026d694621f6FDfD"), // Uniswap V3 (Base)
            new Address("0x4200000000000000000000000000000000000002"), //Aerodrome
            new Address("0x38015D05f4fEC8AFe15D7cc0386a126574e8077B"), //BaseSwap
            new Address("0x25C8a3541e4d986fc31d302220063C5f61Da42E1") //Velodrome
    );

    private static final List<BigInteger> FEE_TIERS = List.of(
            BigInteger.valueOf(500), //0.05%
            BigInteger.valueOf(3000), //0.3%
            BigInteger.valueOf(10000) //1%
    );

    private static final String V3_POOL_BYTECODE_HASH =
            "e34f199b19b2b4f47f408ffe847b1e8b7d4908b51748f25c58653c4f8d97ff9b";

    private static final int AVERAGE_BLOCK_TIME_SECONDS = 2;

    /**
     * Finds the best pool address for tokenA/tokenB: static map first, then dynamic lookup
     */
    public Optional<Address> findBestPool(Address tokenA, Address tokenB) {
        // 1) static CommonPools lookup by symbol key
        String key = symbolOf(tokenA) + "-" + symbolOf(tokenB);
        for (Map<String, String> pools : CommonPools.POOL_ADDRESSES.values()) {
            if (pools.containsKey(key)) {
                return Optional.of(new Address(pools.get(key)));
            }
        }
        // 2) dynamic CREATE2 resolution
        Address[] ordered = lexicographic(tokenA, tokenB);
        for (Address factory : FACTORIES) {
            for (BigInteger fee : FEE_TIERS) {
                Address pool = computePoolAddress(factory, ordered[0], ordered[1], fee);
                if (codeExists(pool)) {
                    return Optional.of(pool);
                }
            }
        }
        return Optional.empty();
    }

    /** Helper to map common token addresses to symbols (for static lookup). */
    public String symbolOf(Address token) {
        // Try reverse lookup in each DEX's pool map
        for (Map.Entry<String, Map<String, String>> dexEntry : CommonPools.POOL_ADDRESSES.entrySet()) {
            for (String key : dexEntry.getValue().keySet()) {
                String address = dexEntry.getValue().get(key);
                if (address.equalsIgnoreCase(token.getValue())) {
                    // key is like "USDC-WETH"
                    return key.split("-")[0];
                }
            }
        }
        // fallback: use hex prefix
        return token.getValue().substring(0, 6);
    }

    /**
     * Computes Uniswap V3 TWAP by calling observe and converting ticks via TickMath.
     */
    public TwapData observeTwap(Address pool, int secondsAgo, BigInteger atBlock) throws Exception {
        DefaultBlockParameterNumber blkNow = new DefaultBlockParameterNumber(atBlock);
        // approximate blocks elapsed
        BigInteger blocksAgo = BigInteger.valueOf(secondsAgo)
                .divide(BigInteger.valueOf(AVERAGE_BLOCK_TIME_SECONDS));
        BigInteger blkPast = atBlock.subtract(blocksAgo);

        // 1) call observe([0, secondsAgo])
        DynamicArray<Uint32> secs = new DynamicArray<>(
                Uint32.class,
                List.of(new Uint32(BigInteger.ZERO), new Uint32(BigInteger.valueOf(secondsAgo)))
        );
        Function observe = new Function(
                "observe",
                List.of(secs),
                List.of(
                        new TypeReference<DynamicArray<Uint256>>() {},
                        new TypeReference<DynamicArray<Uint128>>() {}
                )
        );
        EthCall res = web3j.ethCall(
                Transaction.createEthCallTransaction(null, pool.getValue(), FunctionEncoder.encode(observe)),
                blkNow
        ).send();
        if (res.hasError()) throw new IllegalStateException(res.getError().getMessage());

        var out = FunctionReturnDecoder.decode(res.getValue(), observe.getOutputParameters());
        List<Uint256> ticks = ((DynamicArray<Uint256>) out.get(0)).getValue();
        BigInteger tickNow = ticks.get(1).getValue();
        BigInteger tickPast = ticks.get(0).getValue();
        BigInteger tickDelta = tickNow.subtract(tickPast);

        // 2) average tick and compute sqrtPriceX96 TWAP
        int avgTick = tickDelta.divide(BigInteger.valueOf(secondsAgo)).intValue();
        BigInteger sqrtPriceX96Twap = TickMath.getSqrtRatioAtTick(avgTick);

        // 3) reserve & volume
        BigDecimal reserve = computeReserve(pool, sqrtPriceX96Twap, blkNow);
        BigDecimal volumeUsd = computeSwapVolumeUsd(pool, blkPast, blkNow, sqrtPriceX96Twap);

        return new TwapData(sqrtPriceX96Twap, reserve, volumeUsd);
    }

    /** Converts sqrtPriceX96 to price. */
    public BigDecimal sqrtPriceX96ToPrice(BigInteger sqrtPriceX96, Address token0, Address token1) {
        BigDecimal num = new BigDecimal(sqrtPriceX96).pow(2);
        BigDecimal denom = BigDecimal.valueOf(2).pow(192);
        return num.divide(denom, 18, RoundingMode.HALF_UP);
    }

    /** Derives reserve0 = L^2 / price using TWAP sqrtPriceX96. */
    private BigDecimal computeReserve(Address pool, BigInteger sqrtPriceX96, DefaultBlockParameterNumber blk)
            throws Exception {
        Function liq = new Function("liquidity", List.of(), List.of(new TypeReference<Uint128>() {}));
        EthCall cr = web3j.ethCall(
                Transaction.createEthCallTransaction(null, pool.getValue(), FunctionEncoder.encode(liq)),
                blk
        ).send();
        BigInteger liquidity = ((Uint128) FunctionReturnDecoder.decode(cr.getValue(), liq.getOutputParameters()).get(0)).getValue();
        BigDecimal L = new BigDecimal(liquidity);
        BigDecimal price = new BigDecimal(sqrtPriceX96).pow(2)
                .divide(BigDecimal.valueOf(2).pow(192), 18, RoundingMode.HALF_UP);
        return L.pow(2).divide(price, 18, RoundingMode.HALF_UP);
    }

    /** Sums Swap event volumes, converted to USD at TWAP price. */
    private BigDecimal computeSwapVolumeUsd(
            Address pool,
            BigInteger blkFrom,
            DefaultBlockParameterNumber blkTo,
            BigInteger sqrtPriceX96Twap
    ) throws Exception {
        List<TypeReference<?>> refs = List.of(
                new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {},
                new TypeReference<Int256>() {}, new TypeReference<Int256>() {},
                new TypeReference<Uint160>() {}, new TypeReference<Uint128>() {},
                new TypeReference<Int24>() {}
        );
        Event swapEvt = new Event("Swap", refs);
        EthFilter filter = new EthFilter(
                new DefaultBlockParameterNumber(blkFrom), blkTo, pool.getValue()
        );
        filter.addSingleTopic(EventEncoder.encode(swapEvt));
        List<EthLog.LogResult> logs = web3j.ethGetLogs(filter).send().getLogs();
        BigDecimal price = sqrtPriceX96ToPrice(sqrtPriceX96Twap, null, null);
        BigDecimal total = BigDecimal.ZERO;
        for (EthLog.LogResult<?> lr : logs) {
            Log lg = (Log) lr.get();
            var dec = FunctionReturnDecoder.decode(lg.getData(), swapEvt.getNonIndexedParameters());
            BigInteger a0 = ((Int256) dec.get(0)).getValue().abs();
            BigInteger a1 = ((Int256) dec.get(1)).getValue().abs();
            BigDecimal v0 = new BigDecimal(a0).multiply(price).divide(BigDecimal.TEN.pow(18), 18, RoundingMode.HALF_UP);
            BigDecimal v1 = new BigDecimal(a1).divide(BigDecimal.TEN.pow(18), 18, RoundingMode.HALF_UP);
            total = total.add(v0).add(v1);
        }
        return total;
    }

    private Address computePoolAddress(Address factory, Address token0, Address token1, BigInteger fee) {
        byte[] packed = Bytes.concat(
                Numeric.toBytesPadded(new BigInteger(token0.getValue().substring(2), 16), 20),
                Numeric.toBytesPadded(new BigInteger(token1.getValue().substring(2), 16), 20),
                Numeric.toBytesPadded(fee, 32)
        );
        byte[] salt = Hash.sha3(packed);
        String addr = ContractUtils.generateCreate2ContractAddress(
                factory.getValue(), salt, V3_POOL_BYTECODE_HASH.getBytes()
        );
        return new Address(addr);
    }

    private boolean codeExists(Address addr) {
        try {
            EthGetCode cg = web3j.ethGetCode(addr.getValue(), DefaultBlockParameterName.LATEST).send();
            String code = cg.getCode();
            return code != null && code.length() > 10;
        } catch (Exception e) {
            return false;
        }
    }

//    private BigInteger readSqrtPriceX96(Address pool, BigInteger atBlock) throws Exception {
//        Function slot0 = new Function(
//                "slot0", List.of(), List.of(new TypeReference<Uint160>() {})
//        );
//        EthCall rc = web3j.ethCall(
//                Transaction.createEthCallTransaction(null, pool.getValue(), FunctionEncoder.encode(slot0)),
//                new DefaultBlockParameterNumber(atBlock)
//        ).send();
//        if (rc.hasError()) throw new IllegalStateException(rc.getError().getMessage());
//        return (BigInteger) FunctionReturnDecoder.decode(rc.getValue(), slot0.getOutputParameters()).get(0).getValue();
//    }

    private Address[] lexicographic(Address a, Address b) {
        BigInteger ai = Numeric.toBigInt(a.getValue());
        BigInteger bi = Numeric.toBigInt(b.getValue());
        return ai.compareTo(bi) < 0 ? new Address[]{a, b} : new Address[]{b, a};
    }
}