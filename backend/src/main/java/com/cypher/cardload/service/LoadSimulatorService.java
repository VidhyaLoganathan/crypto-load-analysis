package com.cypher.cardload.service;

import com.cypher.cardload.model.LoadVolumeData;
import com.cypher.cardload.model.TokenTransfer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * This service provides simulated data when blockchain RPC endpoints are unavailable.
 * Useful for development, testing, and demo purposes.
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "load-simulator.enabled", havingValue = "true")
public class LoadSimulatorService {

    private final Random random = new Random();

    // Token addresses for simulation
    private final String[] tokenAddresses = {
            "0x0000000000000000000000000000000000000000", // ETH
            "0x833589fCD6eDb6E08f4c7C32D4f71b54bdA02913", // USDC
            "0x4200000000000000000000000000000000000006", // WETH
            "0x50c5725949A6F0c72E6C4a641F24049A917DB0Cb", // DAI
            "0x7c8dABe453A128639C60e268Acf9E9b4D699a4Bc"  // USDT
    };

    // Token symbols corresponding to addresses
    private final String[] tokenSymbols = {
            "ETH",
            "USDC",
            "WETH",
            "DAI",
            "USDT"
    };

    // Simulated token prices in USD
    private final BigDecimal[] tokenPrices = {
            new BigDecimal("2650.00"),   // ETH
            BigDecimal.ONE,              // USDC
            new BigDecimal("2648.50"),   // WETH
            new BigDecimal("0.999"),     // DAI
            new BigDecimal("0.998")      // USDT
    };

    // Random wallet addresses for simulation
    private final String[] randomWallets = {
            "0x123456789012345678901234567890abcdef1234",
            "0xabcdef1234567890123456789012345678901234",
            "0x9876543210abcdef1234567890123456789abcde",
            "0xfedcba9876543210fedcba9876543210fedcba98",
            "0x0123456789abcdef0123456789abcdef01234567"
    };

    /**
     * Generate simulated token transfers for a date range
     */
    public List<TokenTransfer> generateTransfersForDateRange(LocalDate startDate, LocalDate endDate) {
        List<TokenTransfer> transfers = new ArrayList<>();

        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            // Generate 5-20 transfers per day
            int transfersPerDay = 5 + random.nextInt(16);

            for (int i = 0; i < transfersPerDay; i++) {
                // Random hour of the day
                int hour = random.nextInt(24);
                int minute = random.nextInt(60);
                LocalDateTime timestamp = currentDate.atTime(hour, minute);

                // Random token
                int tokenIndex = random.nextInt(tokenSymbols.length);
                String tokenAddress = tokenAddresses[tokenIndex];
                String tokenSymbol = tokenSymbols[tokenIndex];
                BigDecimal tokenPrice = tokenPrices[tokenIndex];

                // Random amount (between $100 and $10,000)
                BigDecimal usdAmount = new BigDecimal(100 + random.nextInt(9901));

                // Convert to token amount
                BigDecimal tokenAmount = usdAmount.divide(tokenPrice, 6, BigDecimal.ROUND_HALF_UP);

                // Convert to raw amount (with decimals)
                int decimals = tokenSymbol.equals("USDC") || tokenSymbol.equals("USDT") ? 6 : 18;
                BigInteger rawAmount = tokenAmount.multiply(BigDecimal.TEN.pow(decimals)).toBigInteger();

                // Random from address
                String fromAddress = randomWallets[random.nextInt(randomWallets.length)];

                // Create transfer
                TokenTransfer transfer = TokenTransfer.builder()
                        .txHash("0x" + generateRandomHex(64))
                        .tokenAddress(tokenAddress)
                        .tokenSymbol(tokenSymbol)
                        .fromAddress(fromAddress)
                        .amount(rawAmount)
                        .usdValue(usdAmount)
                        .timestamp(timestamp)
                        .decimals(decimals)
                        .build();

                transfers.add(transfer);
            }

            currentDate = currentDate.plusDays(1);
        }

        log.info("Generated {} simulated transfers between {} and {}",
                transfers.size(), startDate, endDate);

        return transfers;
    }

    /**
     * Generate simulated load volume data for a date range
     */
    public List<LoadVolumeData> generateDailyLoadVolumeData(LocalDate startDate, LocalDate endDate) {
        List<LoadVolumeData> result = new ArrayList<>();

        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            // Generate daily volume between $10,000 and $100,000
            BigDecimal totalVolume = new BigDecimal(10000 + random.nextInt(90001));

            // Generate token breakdown
            Map<String, BigDecimal> tokenBreakdown = generateTokenBreakdown(totalVolume);

            LoadVolumeData volumeData = LoadVolumeData.builder()
                    .date(currentDate)
                    .totalUsdValue(totalVolume)
                    .tokenBreakdown(tokenBreakdown)
                    .build();

            result.add(volumeData);
            currentDate = currentDate.plusDays(1);
        }

        return result;
    }

    /**
     * Generate simulated weekly load volume data
     */
    public List<LoadVolumeData> generateWeeklyLoadVolumeData(LocalDate startDate, LocalDate endDate) {
        List<LoadVolumeData> result = new ArrayList<>();

        // Adjust start date to beginning of week
        LocalDate weekStart = startDate.minusDays(startDate.getDayOfWeek().getValue() - 1);

        while (!weekStart.isAfter(endDate)) {
            // Weekly end date is either end of week or endDate, whichever is earlier
            LocalDate weekEnd = weekStart.plusDays(6);
            if (weekEnd.isAfter(endDate)) {
                weekEnd = endDate;
            }

            // Weekly volume between $70,000 and $700,000
            BigDecimal totalVolume = new BigDecimal(70000 + random.nextInt(630001));

            // Generate token breakdown
            Map<String, BigDecimal> tokenBreakdown = generateTokenBreakdown(totalVolume);

            LoadVolumeData volumeData = LoadVolumeData.builder()
                    .date(weekStart)  // Use week start date
                    .totalUsdValue(totalVolume)
                    .tokenBreakdown(tokenBreakdown)
                    .build();

            result.add(volumeData);
            weekStart = weekStart.plusWeeks(1);
        }

        return result;
    }

    /**
     * Generate simulated monthly load volume data
     */
    public List<LoadVolumeData> generateMonthlyLoadVolumeData(LocalDate startDate, LocalDate endDate) {
        List<LoadVolumeData> result = new ArrayList<>();

        // Adjust start date to beginning of month
        LocalDate monthStart = startDate.withDayOfMonth(1);

        while (!monthStart.isAfter(endDate)) {
            // Monthly end date is either end of month or endDate, whichever is earlier
            LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
            if (monthEnd.isAfter(endDate)) {
                monthEnd = endDate;
            }

            // Monthly volume between $300,000 and $3,000,000
            BigDecimal totalVolume = new BigDecimal(300000 + random.nextInt(2700001));

            // Generate token breakdown
            Map<String, BigDecimal> tokenBreakdown = generateTokenBreakdown(totalVolume);

            LoadVolumeData volumeData = LoadVolumeData.builder()
                    .date(monthStart)  // Use month start date
                    .totalUsdValue(totalVolume)
                    .tokenBreakdown(tokenBreakdown)
                    .build();

            result.add(volumeData);
            monthStart = monthStart.plusMonths(1);
        }

        return result;
    }

    /**
     * Generate simulated summary data for a date range
     */
    public LoadVolumeData generateSummaryData(LocalDate startDate, LocalDate endDate) {
        // Total volume based on date range length
        long days = startDate.until(endDate).getDays() + 1;
        BigDecimal baseVolume = new BigDecimal(50000 * days);
        BigDecimal randomFactor = new BigDecimal(0.5 + random.nextDouble());
        BigDecimal totalVolume = baseVolume.multiply(randomFactor);

        // Generate token breakdown
        Map<String, BigDecimal> tokenBreakdown = generateTokenBreakdown(totalVolume);

        return LoadVolumeData.builder()
                .date(startDate)
                .totalUsdValue(totalVolume)
                .tokenBreakdown(tokenBreakdown)
                .build();
    }

    // Helper method to generate token breakdown
    private Map<String, BigDecimal> generateTokenBreakdown(BigDecimal totalAmount) {
        Map<String, BigDecimal> breakdown = new HashMap<>();

        // Assign random percentages to each token
        double ethPercent = 0.40 + random.nextDouble() * 0.20;  // 40-60%
        double usdcPercent = 0.20 + random.nextDouble() * 0.15; // 20-35%
        double wethPercent = 0.05 + random.nextDouble() * 0.10; // 5-15%
        double daiPercent = 0.05 + random.nextDouble() * 0.10;  // 5-15%

        // USDT gets the remainder
        double usdtPercent = 1.0 - ethPercent - usdcPercent - wethPercent - daiPercent;

        breakdown.put("ETH", totalAmount.multiply(new BigDecimal(ethPercent)));
        breakdown.put("USDC", totalAmount.multiply(new BigDecimal(usdcPercent)));
        breakdown.put("WETH", totalAmount.multiply(new BigDecimal(wethPercent)));
        breakdown.put("DAI", totalAmount.multiply(new BigDecimal(daiPercent)));
        breakdown.put("USDT", totalAmount.multiply(new BigDecimal(usdtPercent)));

        return breakdown;
    }

    // Helper method to generate random hex string
    private String generateRandomHex(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append("0123456789abcdef".charAt(random.nextInt(16)));
        }
        return sb.toString();
    }
}