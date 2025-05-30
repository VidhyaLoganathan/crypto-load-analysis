package com.cypher.cardload.config;

import java.util.Map;

/**
 * Pre-configured on-chain pool addresses for our most liquid pairs,
 * keyed by DEX name and token pair ("TOKEN1-TOKEN2").
 */
public final class CommonPools {
    public static final Map<String, Map<String, String>> POOL_ADDRESSES = Map.of(
            // Aerodrome V3 pools on Base network
            "Aerodrome", Map.of(
                    "USDC-WETH", "0x3548029694fbb241d45fb24ba0cd9c9d4e745f16",  // WETH/USDC Aerodrome Base
                    "USDT-WETH", "0x9785ef59e2b499fb741674ecf6faf912df7b3c1b", // USDT/WETH Aerodrome Base
                    "DAI-WETH", "0x9287c921f5d920ceee0d07d7c58d476e46acc640", // DAI/WETH Aerodrome Base
                    "USDC-USDT", "0xa41bc0affba7fd420d186b84899d7ab2ac57fcd1", // USDT/USDC Aerodrome Base
                    "DAI-USDC", "0x67b00b46fa4f4f24c03855c5c8013c0b938b3eec", // DAI/USDC Aerodrome Base
                    "sFRAX-USDC", "0xf0e08fa759b9c3099b0dd63f73f79984aeb6254f", // sFRAX/USDC Aerodrome Base
                    "FXS-USDC", "0xa2d4414421e5531e727d532ef018109f59d22340"  // FXS/USDC Aerodrome Base
            ),
            // BaseSwap V3 pools on Base network
            "BaseSwap", Map.of(
                    "USDC-WETH", "0x74cb6260be6f31965c239df6d6ef2ac2b5d4f020",  // WETH/USDC BaseSwap V3 0.05% fee
                    "DAI-WETH", "0x0feb1490f80b6978002c3e501753562f2f2853b2",  // DAI/WETH BaseSwap V3 0.3% fee
                    "USDC-USDT", "0x0653cac064eabba25ce25b415cc0767b532006d0",  // USDT/USDC BaseSwap V3 0.005% fee
                    "DAI-USDC", "0x8b7cc11ff640a494e5a31a82415bb0d831e62363",   // DAI/USDC BaseSwap V3 0.05% fee
                    "axlWBTC-USDbC", "0x317d373e590795e2c09d73fad7498fc98c0a692b", // axlWBTC/USDbC BaseSwap V3
                    "cbBTC-USDC", "0x3efaed538d2d68251ee3f466e7738c39dc77483a"  // DAI/USDC BaseSwap V3 0.05% fee
            ),
            // Velodrome V3 pools on Base network
            "Velodrome", Map.of(
                    "USDC-WETH", "0x79c912fef520be002c2b6e57ec4324e260f38e50",  // USDC/WETH Velodrome V3
                    "VELO-WETH", "0xecba8a8e98624f275c6e27067145abf1038a9fc5",   // VELO/WETH Velodrome V3
                    "USDC-USDT", "0xe08d427724d8a2673fe0be3a81b7db17be835b36", // USDC/USDT Velodrome V3
                    "VELO-USDC", "0xe8537b6ff1039cb9ed0b71713f697ddbadbb717d", // VELO/USDC Velodrome V3
                    "OP-USDC", "0x47029bc8f5cbe3b464004e87ef9c9419a48018cd", // OP/USDC Velodrome
                    "MAI-USDT", "0x4ab66eed0b39f9d229a1281468a4482515db0709"  // MAI/USDT Velodrome
            )
    );

    private CommonPools() {
    }
}
