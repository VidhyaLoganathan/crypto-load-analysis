package com.cypher.cardload.util;
import java.util.HashMap;
import java.util.Map;

public class BlockchainConstants {

    // Cypher Master Wallet
    public static final String CYPHER_MASTER_WALLET = "0xcCCd218A58B53C67fC17D8C87Cb90d83614e35fD";

    // DEX Routers on Base Chain
    public static final Map<String, String> DEX_ROUTERS = createDexRoutersMap();

    // Common Base Chain Bridges
    public static final Map<String, String> BRIDGES = createBridgesMap();

    // Major CEX Hot Wallets
    public static final Map<String, String> CEX_WALLETS = createCexWalletsMap();

    // Common token addresses on Base chain
    public static final Map<String, String> TOKEN_ADDRESSES = createTokenAddressesMap();

    // Popular protocols on Base chain
    public static final Map<String, String> PROTOCOLS = createProtocolsMap();

    /**
     * Create the DEX routers map with known addresses
     * @return Map of DEX router addresses to names
     */
    private static Map<String, String> createDexRoutersMap() {
        Map<String, String> map = new HashMap<>();
        map.put("0x4cdf24e0584985c94879e85cd6ac5e25f31d5eca", "Aerodrome Router");
        map.put("0xbbbc1f6be7b76a15b3532a2b27d26c8ca96eade8", "Uniswap V3 Router");
        map.put("0xc30141b657f4216252dc59af2e7cdb9d8792e1b0", "Uniswap V3 Router 2");
        map.put("0xdc0b52c04f6a87fae581ef4bf47ce9bc6508a646", "BaseX Router");
        map.put("0x81c9a7b8a0a64118b41c51c5d888d8c8be81c444", "Baseswap Router");
        map.put("0x8355100ef5014d2a4b15a7d2ec02c966296cb570", "Maverick Router");
        map.put("0x123ff0e0f9c1da6a21c83b3454390453c604b79b", "SushiSwap Router");
        map.put("0x9d1b1669c73b033dfe47ae5a0164ab96df25b944", "DackieSwap Router");
        map.put("0xf73815d846b93e752f648dc0b7f3eb6e5656a32a", "Cypher Protocol");
        return map;
    }

    /**
     * Create the bridges map with known addresses
     * @return Map of bridge addresses to names
     */
    private static Map<String, String> createBridgesMap() {
        Map<String, String> map = new HashMap<>();
        map.put("0xc5af84701f98fa483ece78af83f11b6c38aca71d", "Base Bridge");
        map.put("0x3154cf16ccdb4c6d922629664174b904d80f2c35", "Base Bridge (L1)");
        map.put("0x391d18ce947c88174342e74be8e6c0cef10c3268", "Aero Bridge");
        map.put("0xdec800c7361497f7b80d898d8b466a7f52bc663b", "deBridge");
        map.put("0x3d4e44eb1374240ce5f1b871ab261cd16335b76a", "Stargate Finance");
        map.put("0x8731d54e9d02c286767d56ac03e8037c07e01e98", "Wormhole");
        map.put("0x6ef95b6f3b323d417e5b8d57e343d7c2976c8b35", "Base Bridge");
        return map;
    }

    /**
     * Create the CEX wallets map with known addresses
     * @return Map of CEX wallet addresses to names
     */
    private static Map<String, String> createCexWalletsMap() {
        Map<String, String> map = new HashMap<>();
        map.put("0x28c6c06298d514db089934071355e5743bf21d60", "Binance Hot Wallet");
        map.put("0xddfabcdc4d8ffc6d5beaf154f18b778f892a0740", "Coinbase Hot Wallet");
        map.put("0x881d40237659c251811cec9c364ef91dc08d300c", "Kraken Hot Wallet");
        map.put("0x5bdf85216ec1e38e6c9c5fbd5dd5e20e5b72a9a4", "OKX Hot Wallet");
        map.put("0x6cc5f688a315f3dc28a7781717a9a798a59fda7b", "KuCoin Hot Wallet");
        map.put("0x59fb92f9113f24db068f6a2e44dd9382a50afe9f", "Crypto.com Hot Wallet");
        map.put("0x989a8604c55b48c722ddf2d67b63a59e006f35c5", "Gate.io Hot Wallet");
        return map;
    }

    /**
     * Create the token addresses map with known addresses
     * @return Map of token addresses to names
     */
    private static Map<String, String> createTokenAddressesMap() {
        Map<String, String> map = new HashMap<>();
        map.put("0x833589fcd6edb6e08f4c7c32d4f71b54bda02913", "USDC");
        map.put("0x50c5725949a6f0c72e6c4a641f24049a917db0cb", "DAI");
        map.put("0x4200000000000000000000000000000000000006", "WETH");
        map.put("0x4158734d47fc9692176b5085e0f52ee0da5d47f1", "USDT");
        map.put("0x2ae3f1ec7f1f5012cfe6c6ae0d34b7b9e61dd2eb", "cbETH");
        map.put("0x78a087d713be7d1df7715a9f2ed2b63851f24e20", "BALD");
        map.put("0xd9aAEc86B65D86f6A7B5B1b0c42FFA531710b6CA", "USDbC");
        map.put("0x9e1028f5f1d5ede59748ffcee5532509976840e0", "COMP");
        map.put("0x27d2decb4bfc9c76f0309b8e88dec3a601fe25a8", "DEGEN");
        map.put("0xeb466342c4d449bc9f53a865d5cb90586f405215", "axlUSDC");
        return map;
    }

    /**
     * Create the protocols map with known protocol addresses
     * @return Map of protocol addresses to names
     */
    private static Map<String, String> createProtocolsMap() {
        Map<String, String> map = new HashMap<>();

        // DeFi protocols
        map.put("0xf73815d846b93e752f648dc0b7f3eb6e5656a32a", "Cypher Protocol");
        map.put("0x5bbce817d4f7d7f48783b95c9e6043525db9a316", "Radius Finance");
        map.put("0x1cba2cd927310dab9e762cf8ddb233889d1b00fb", "Compound");
        map.put("0x0d3e16e9791549cd1d8ffbb72ab342bd7c12d4e3", "Aave");
        map.put("0x1d93d31293374dcb49df6b9e14404876afd1d561", "Flux Finance");
        map.put("0x703b52d2b0e9bb784623d9a0f3b3d7eda5be0967", "Balancer");
        map.put("0xd4e45fbcb4991f1be1d09a71731ca5afcf106358", "Overnight Finance");
        map.put("0x9e8487ea947a327596af1ef9eff6fc07fb8bfde9", "Beefy Finance");
        map.put("0x03c20a2d9a0adf5bbf588c3673c201d99b320da0", "Yearn Finance");
        map.put("0xf4f5256c15c380ed5fc25278fb36b5423192faec", "Curve Finance");

        // NFT and Gaming protocols
        map.put("0x91fd5a14a21dc9cf507546e6867d1fe8ee3bc2b9", "Base Punks");
        map.put("0x2d95c7ec4c31d3ed534e9e40afd1901c962254b3", "Friend.tech");
        map.put("0xb4fbf271143f4fbf7b91a5ded31805e42b2208d6", "Quests");
        map.put("0xa902a7cc88a75192b4b09c92061d9d774a29f8bf", "Parallel");

        // Oracles
        map.put("0x71041dddad3595f9ced3dccfbe3d1f4b0a16bb70", "Chainlink");
        map.put("0x3c4f787c29d5ae9d56e2ff134cf63bd9f523ff89", "Pyth Network");

        // Add all DEX routers
        map.putAll(DEX_ROUTERS);

        // Add all bridges
        map.putAll(BRIDGES);

        return map;
    }

    /**
     * Get a merged map of all known addresses and their identifiers
     *
     * @return Map of all known addresses to their names/identifiers
     */
    public static Map<String, String> getAllKnownAddresses() {
        Map<String, String> allAddresses = new HashMap<>();

        // Add CEX wallets
        allAddresses.putAll(CEX_WALLETS);

        // Add token addresses
        allAddresses.putAll(TOKEN_ADDRESSES);

        // Add protocol addresses (which already include DEX routers and bridges)
        allAddresses.putAll(PROTOCOLS);

        // Add Cypher master wallet
        allAddresses.put(CYPHER_MASTER_WALLET.toLowerCase(), "Cypher Master Wallet");

        return allAddresses;
    }
}