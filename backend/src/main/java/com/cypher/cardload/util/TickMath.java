package com.cypher.cardload.util;

import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * Java port of Uniswap V3's TickMath library.
 * Computes sqrtPriceX96 ↔ tick conversions in Q64.96 fixed point.
 */
public final class TickMath {
    private static final BigInteger MIN_SQRT_RATIO = new BigInteger("4295128739");
    private static final BigInteger MAX_SQRT_RATIO = new BigInteger("1461446703485210103287273052203988822378723970342");

    public static final int MIN_TICK = -887272;
    public static final int MAX_TICK = -MIN_TICK;

    private TickMath() {}

    /**
     * Returns sqrt(1.0001^tick) * 2^96.
     * @param tick must be in [MIN_TICK, MAX_TICK]
     */
    public static BigInteger getSqrtRatioAtTick(int tick) {
        if (tick < MIN_TICK || tick > MAX_TICK) {
            throw new IllegalArgumentException("Tick out of range");
        }
        int absTick = tick < 0 ? -tick : tick;
        BigInteger ratio = BigInteger.ONE.shiftLeft(128);

        if ((absTick & 0x1) != 0)
            ratio = ratio.multiply(new BigInteger("fffcb933bd6fad37aa2d162d1a594001", 16)).shiftRight(128);
        if ((absTick & 0x2) != 0)
            ratio = ratio.multiply(new BigInteger("fff97272373d413259a46990580e213a", 16)).shiftRight(128);
        if ((absTick & 0x4) != 0)
            ratio = ratio.multiply(new BigInteger("fff2e50f5f656932ef12357cf3c7fdcc", 16)).shiftRight(128);
        if ((absTick & 0x8) != 0)
            ratio = ratio.multiply(new BigInteger("ffe5caca7e10e4e61c3624eaa0941cd0", 16)).shiftRight(128);
        if ((absTick & 0x10) != 0)
            ratio = ratio.multiply(new BigInteger("ffcb9843d60f6159c9db58835c926644", 16)).shiftRight(128);
        if ((absTick & 0x20) != 0)
            ratio = ratio.multiply(new BigInteger("ff973b41fa98c081472e6896dfb254c0", 16)).shiftRight(128);
        if ((absTick & 0x40) != 0)
            ratio = ratio.multiply(new BigInteger("ff2ea16466c96a3843ec78b326b52861", 16)).shiftRight(128);
        if ((absTick & 0x80) != 0)
            ratio = ratio.multiply(new BigInteger("fe5dee046a99a2a811c461f1969c3053", 16)).shiftRight(128);
        if ((absTick & 0x100) != 0)
            ratio = ratio.multiply(new BigInteger("fcbe86c7900a88aedcffc83b479aa3a4", 16)).shiftRight(128);
        if ((absTick & 0x200) != 0)
            ratio = ratio.multiply(new BigInteger("f987a7253ac413176f2b074cf7815e54", 16)).shiftRight(128);
        if ((absTick & 0x400) != 0)
            ratio = ratio.multiply(new BigInteger("f3392b0822b70005940c7a398e4b70f3", 16)).shiftRight(128);
        if ((absTick & 0x800) != 0)
            ratio = ratio.multiply(new BigInteger("e7159475a2c29b7443b29c7fa6e889d9", 16)).shiftRight(128);
        if ((absTick & 0x1000) != 0)
            ratio = ratio.multiply(new BigInteger("d097f3bdfd2022b8845ad8f792aa5825", 16)).shiftRight(128);
        if ((absTick & 0x2000) != 0)
            ratio = ratio.multiply(new BigInteger("a9f746462d870fdf8a65dc1f90e061e5", 16)).shiftRight(128);
        if ((absTick & 0x4000) != 0)
            ratio = ratio.multiply(new BigInteger("70d869a156d2a1b890bb3df62baf32f7", 16)).shiftRight(128);
        if ((absTick & 0x8000) != 0)
            ratio = ratio.multiply(new BigInteger("31be135f97d08fd981231505542fcfa6", 16)).shiftRight(128);
        if ((absTick & 0x10000) != 0)
            ratio = ratio.multiply(new BigInteger("9aa508b5b7a84e1c677de54f3e99bc9", 16)).shiftRight(128);
        if ((absTick & 0x20000) != 0)
            ratio = ratio.multiply(new BigInteger("5d6af8dedb81196699c329225ee604", 16)).shiftRight(128);
        if ((absTick & 0x40000) != 0)
            ratio = ratio.multiply(new BigInteger("2216e584f5fa1ea926041bedfe98", 16)).shiftRight(128);
        if ((absTick & 0x80000) != 0)
            ratio = ratio.multiply(new BigInteger("48a170391f7dc42444e8fa2", 16)).shiftRight(128);

        if (tick > 0) {
            ratio = BigInteger.ONE.shiftLeft(256).divide(ratio);
        }
        BigInteger sqrtPriceX96 = ratio.shiftRight(32);
        if (sqrtPriceX96.compareTo(MIN_SQRT_RATIO) < 0 || sqrtPriceX96.compareTo(MAX_SQRT_RATIO) > 0) {
            throw new IllegalStateException("Computed sqrtPriceX96 out of bounds");
        }
        return sqrtPriceX96;
    }

    /**
     * Returns the tick for a given sqrtPriceX96.
     * @param sqrtPriceX96 must be in [MIN_SQRT_RATIO, MAX_SQRT_RATIO]
     */
    public static int getTickAtSqrtRatio(BigInteger sqrtPriceX96) {
        if (sqrtPriceX96.compareTo(MIN_SQRT_RATIO) < 0 || sqrtPriceX96.compareTo(MAX_SQRT_RATIO) > 0) {
            throw new IllegalArgumentException("sqrtPriceX96 out of range");
        }
        int msb = sqrtPriceX96.bitLength() - 1;
        BigInteger r = (msb >= 128)
                ? sqrtPriceX96.shiftRight(msb - 127)
                : sqrtPriceX96.shiftLeft(127 - msb);

        BigInteger log2 = BigInteger.valueOf(msb - 128).shiftLeft(64);
        for (int i = 0; i < 64; ++i) {
            r = r.multiply(r).shiftRight(127);
            if (r.compareTo(BigInteger.ONE.shiftLeft(128)) >= 0) {
                r = r.shiftRight(1);
                log2 = log2.or(BigInteger.ONE.shiftLeft(63 - i));
            }
        }
        BigInteger log_sqrt10001 = log2
                .multiply(new BigInteger("255738958999603826347141"))
                .shiftRight(128);
        BigInteger multiplier = new BigInteger("291339464771989622907027621153398088495");
        BigInteger base = new BigInteger("3402992956809132418596140100660247210");
        int tickLow = log_sqrt10001
                .subtract(base)
                .divide(multiplier)
                .intValue();
        int tickHigh = log_sqrt10001
                .add(multiplier)
                .divide(multiplier)
                .intValue();
        return (tickLow == tickHigh)
                ? tickLow
                : getSqrtRatioAtTick(tickHigh).compareTo(sqrtPriceX96) <= 0
                ? tickHigh
                : tickLow;
    }
}
