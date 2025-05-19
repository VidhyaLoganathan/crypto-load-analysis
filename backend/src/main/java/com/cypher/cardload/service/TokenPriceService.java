package com.cypher.cardload.service;

import com.cypher.cardload.config.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenPriceService {

    private final Web3j web3j;

    @Cacheable("ethUsdPrices")
    public BigDecimal getEthUsdPrice(LocalDateTime timestamp) {
        // Call Aerodrome pool for ETH/USDC
        try {
            String poolAddress = Constants.AERODROME_POOL_ADDRESSES.get("USDC-ETH");
            BigInteger[] prices = getPoolPrices(poolAddress);

            // For ETH/USDC, price0 is ETH and price1 is USDC
            // Convert to price of 1 ETH in USDC
            BigDecimal ethPriceInUsdc = new BigDecimal(prices[1])
                    .multiply(new BigDecimal(10).pow(18)) // ETH has 18 decimals
                    .divide(new BigDecimal(prices[0]).multiply(new BigDecimal(10).pow(6)), 18, RoundingMode.HALF_UP); // USDC has 6 decimals

            return ethPriceInUsdc;
        } catch (Exception e) {
            log.error("Error getting ETH price: ", e);
            // Fallback price - in a real app, we'd use a price oracle or API
            return new BigDecimal("2650.00");
        }
    }

    @Cacheable("tokenUsdPrices")
    public BigDecimal getTokenUsdPrice(String tokenAddress, LocalDateTime timestamp) {
        // Check if it's a known token
        if (Constants.TOKEN_ADDRESSES.get("ETH").equalsIgnoreCase(tokenAddress) ||
                Constants.TOKEN_ADDRESSES.get("WETH").equalsIgnoreCase(tokenAddress)) {
            return getEthUsdPrice(timestamp);
        }

        if (Constants.TOKEN_ADDRESSES.get("USDC").equalsIgnoreCase(tokenAddress) ||
                Constants.TOKEN_ADDRESSES.get("USDT").equalsIgnoreCase(tokenAddress)) {
            // Stablecoins with approximate 1:1 USD peg
            return BigDecimal.ONE;
        }

        if (Constants.TOKEN_ADDRESSES.get("DAI").equalsIgnoreCase(tokenAddress)) {
            // DAI also has an approximate 1:1 USD peg, but we'll get it from the pool for accuracy
            try {
                String poolAddress = Constants.AERODROME_POOL_ADDRESSES.get("DAI-USDC");
                BigInteger[] prices = getPoolPrices(poolAddress);

                // For DAI/USDC, convert to price of 1 DAI in USDC
                BigDecimal daiPriceInUsdc = new BigDecimal(prices[1])
                        .multiply(new BigDecimal(10).pow(18)) // DAI has 18 decimals
                        .divide(new BigDecimal(prices[0]).multiply(new BigDecimal(10).pow(6)), 18, RoundingMode.HALF_UP); // USDC has 6 decimals

                return daiPriceInUsdc;
            } catch (Exception e) {
                log.error("Error getting DAI price: ", e);
                // Fallback price for DAI
                return new BigDecimal("0.999");
            }
        }

        // For other tokens, we'd implement additional price sources
        // Fallback to a default value for demo purposes
        return BigDecimal.ONE;
    }

    // Helper method to get prices from an Aerodrome/Uniswap V3 pool
    private BigInteger[] getPoolPrices(String poolAddress) throws Exception {
        // Call the slot0() function on the pool
        Function function = new Function(
                "slot0",
                Collections.emptyList(),
                Arrays.asList(
                        new TypeReference<Uint256>() {}, // sqrtPriceX96
                        new TypeReference<Uint256>() {}, // tick
                        new TypeReference<Uint256>() {}, // observationIndex
                        new TypeReference<Uint256>() {}, // observationCardinality
                        new TypeReference<Uint256>() {}, // observationCardinalityNext
                        new TypeReference<Uint256>() {}, // feeProtocol
                        new TypeReference<Uint256>() {}  // unlocked
                )
        );

        String encodedFunction = FunctionEncoder.encode(function);

        Transaction transaction = Transaction.createEthCallTransaction(
                "0x0000000000000000000000000000000000000000",
                poolAddress,
                encodedFunction
        );

        EthCall ethCall = web3j.ethCall(transaction, DefaultBlockParameterName.LATEST).send();

        if (ethCall.hasError()) {
            throw new RuntimeException("Error calling pool contract: " + ethCall.getError().getMessage());
        }

        List<Type> results = FunctionReturnDecoder.decode(ethCall.getValue(), function.getOutputParameters());

        if (results.size() < 2) {
            throw new RuntimeException("Invalid response from pool contract");
        }

        BigInteger sqrtPriceX96 = (BigInteger) results.get(0).getValue();
        BigInteger tick = (BigInteger) results.get(1).getValue();

        // Calculate price0 and price1 from sqrtPriceX96
        BigInteger price0 = calculatePrice0(sqrtPriceX96);
        BigInteger price1 = calculatePrice1(sqrtPriceX96);

        return new BigInteger[] { price0, price1 };
    }

    // Helper methods to calculate prices from sqrtPriceX96
    private BigInteger calculatePrice0(BigInteger sqrtPriceX96) {
        // price0 = (sqrtPriceX96 ^ 2) / 2^192
        BigDecimal sqrtPrice = new BigDecimal(sqrtPriceX96);
        BigDecimal squared = sqrtPrice.multiply(sqrtPrice);
        BigDecimal divisor = new BigDecimal(BigInteger.ONE.shiftLeft(192)); // 2^192

        return squared.divide(divisor, 0, RoundingMode.HALF_UP).toBigInteger();
    }

    private BigInteger calculatePrice1(BigInteger sqrtPriceX96) {
        // price1 = 2^192 / (sqrtPriceX96 ^ 2)
        BigDecimal sqrtPrice = new BigDecimal(sqrtPriceX96);
        BigDecimal squared = sqrtPrice.multiply(sqrtPrice);
        BigDecimal dividend = new BigDecimal(BigInteger.ONE.shiftLeft(192)); // 2^192

        if (squared.compareTo(BigDecimal.ZERO) == 0) {
            return BigInteger.ZERO;
        }

        return dividend.divide(squared, 0, RoundingMode.HALF_UP).toBigInteger();
    }
}


