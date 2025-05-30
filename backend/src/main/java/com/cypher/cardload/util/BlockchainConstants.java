package com.cypher.cardload.util;

import org.web3j.abi.datatypes.Address;

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
     *
     * @return Map of DEX router addresses to names
     */
    private static Map<String, String> createDexRoutersMap() {
        Map<String, String> map = new HashMap<>();
//        map.put("0xcF77a3Ba9A5CA399B7c97c74d54e5b1Beb874E43", "Aerodrome Router");
//        map.put("0xbbbc1f6be7b76a15b3532a2b27d26c8ca96eade8", "Uniswap V3 Router");
//        map.put("0xc30141b657f4216252dc59af2e7cdb9d8792e1b0", "Uniswap V3 Router 2");
//        map.put("0xdc0b52c04f6a87fae581ef4bf47ce9bc6508a646", "BaseX Router");
//        map.put("0x81c9a7b8a0a64118b41c51c5d888d8c8be81c444", "Baseswap Router");
//        map.put("0x8355100ef5014d2a4b15a7d2ec02c966296cb570", "Maverick Router");
//        map.put("0x123ff0e0f9c1da6a21c83b3454390453c604b79b", "SushiSwap Router");
//        map.put("0x9d1b1669c73b033dfe47ae5a0164ab96df25b944", "DackieSwap Router");
        return map;
    }

    /**
     * Create the bridges map with known addresses
     *
     * @return Map of bridge addresses to names
     */
    private static Map<String, String> createBridgesMap() {
        Map<String, String> map = new HashMap<>();
//        map.put("0xc5af84701f98fa483ece78af83f11b6c38aca71d", "Base Bridge");
//        map.put("0x3154cf16ccdb4c6d922629664174b904d80f2c35", "Base Bridge (L1)");
//        map.put("0x391d18ce947c88174342e74be8e6c0cef10c3268", "Aero Bridge");
//        map.put("0xdec800c7361497f7b80d898d8b466a7f52bc663b", "deBridge");
//        map.put("0x3d4e44eb1374240ce5f1b871ab261cd16335b76a", "Stargate Finance");
//        map.put("0x8731d54e9d02c286767d56ac03e8037c07e01e98", "Wormhole");
//        map.put("0x6ef95b6f3b323d417e5b8d57e343d7c2976c8b35", "Base Bridge");
        return map;
    }

    /**
     * Create the CEX wallets map with known addresses
     *
     * @return Map of CEX wallet addresses to names
     */
    private static Map<String, String> createCexWalletsMap() {
        Map<String, String> map = new HashMap<>();
//        map.put("0x28c6c06298d514db089934071355e5743bf21d60", "Binance Hot Wallet");
//        map.put("0xddfabcdc4d8ffc6d5beaf154f18b778f892a0740", "Coinbase Hot Wallet");
//        map.put("0x881d40237659c251811cec9c364ef91dc08d300c", "Kraken Hot Wallet");
//        map.put("0x5bdf85216ec1e38e6c9c5fbd5dd5e20e5b72a9a4", "OKX Hot Wallet");
//        map.put("0x6cc5f688a315f3dc28a7781717a9a798a59fda7b", "KuCoin Hot Wallet");
//        map.put("0x59fb92f9113f24db068f6a2e44dd9382a50afe9f", "Crypto.com Hot Wallet");
//        map.put("0x989a8604c55b48c722ddf2d67b63a59e006f35c5", "Gate.io Hot Wallet");
        return map;
    }

    /**
     * Create the token addresses map with known addresses
     * verfied using basescan "https://basescan.org/address/<value></value>" api
     * @return Map of token addresses to names
     */
    public static Map<String, String> createTokenAddressesMap() {
        Map<String, String> map = new HashMap<>();
        map.put("0x833589fcd6edb6e08f4c7c32d4f71b54bda02913", "USDC");
        map.put("0x50c5725949a6f0c72e6c4a641f24049a917db0cb", "DAI");
        map.put("0xfde4C96c8593536E31F229EA8f37b2ADa2699bb2","USDT");
        map.put("0x4200000000000000000000000000000000000006", "WETH");
        map.put("0x04C0599Ae5A44757c0af6F9eC3b93da8976c150A","weETH.base"); //wrapped ether on base
        map.put("0x820C137fa70C8691f0e44Dc420a5e53c168921Dc","USDS");
        map.put("0x63706e401c06ac8513145b7687A14804d17f814b","AAVE");
        map.put("0x940181a94A35A4569E4529A3CDfB74e38FD98631","AERO");
        map.put("0x2Ae3F1Ec7F1F5012CFEab0185bfc7aa3cf0DEc22", "cbETH");
        map.put("0x27D2DECb4bFC9C76F0309b8E88dec3a601Fe25a8", "BALD");
        map.put("0xA62C3B22165f9b84B706Df319D5dFffA050b1cc9", "ERC20**");

        map.put("0xd9aAEc86B65D86f6A7B5B1b0c42FFA531710b6CA", "USDbC");
        map.put("0x9e1028f5f1d5ede59748ffcee5532509976840e0", "COMP");
        map.put("0x4ed4E862860beD51a9570b96d89aF5E1B0Efefed", "DEGEN");
        map.put("0xeb466342c4d449bc9f53a865d5cb90586f405215", "axlUSDC");
        map.put("0x368181499736d0c0cc614dbb145e2ec1ac86b8c6","LUSD");
        map.put("0xB79DD08EA68A908A97220C76d19A6aA9cBDE4376","USD+");
        map.put("0x0000000000000000000000000000000000000000","ETH");

        return map;
    }

    /**
     * Create the protocols map with known protocol addresses
     *
     * @return Map of protocol addresses to names
     */
    private static Map<String, String> createProtocolsMap() {
        Map<String, String> map = new HashMap<>();

//        // Common known address
//        map.put("0x3fc91a3afd70395cd496c647d5a6cc9d4b2b7fad", "Uniswap V3 Router");
//        map.put("0x80c67432656d59144ceff962e8faf8926599bcf8", "Orbiter Finance");
//        map.put("0x49048044d57e1c92a77f79988d21fa8faf74e97e", "Base Name Service");
//        map.put("0x4e59b44847b379578588920ca78fbf26c0b4956c", "Create2 Factory");
//
//        map.put("0x4200000000000000000000000000000000000006", "Base Bridge");
//        map.put("0x2ae3f1ec7f1f5012cfeab0185bfc7aa3cf0dec22", "Coinbase");
//        map.put("0xc1e92bd5d1aa6e5f5f299d0490befd9d8e5a887a", "Binance Deposit");
//
//        map.put("0xba5e05cb26b78eda3a2f8e3b3814726305dcac83", "Aerodrome Finance");
//        map.put("0x9c58bacc331c9aa871afd802db6379a98e80cedb", "Gnosis Safe Proxy");
//        map.put("0xdef1c0ded9bec7f1a1670819833240f027b25eff", "0x Exchange Proxy");
//
//        // DeFi protocols
//        map.put("0x5bbce817d4f7d7f48783b95c9e6043525db9a316", "Radius Finance");
//        map.put("0x1cba2cd927310dab9e762cf8ddb233889d1b00fb", "Compound");
//        map.put("0x0d3e16e9791549cd1d8ffbb72ab342bd7c12d4e3", "Aave");
//        map.put("0x1d93d31293374dcb49df6b9e14404876afd1d561", "Flux Finance");
//        map.put("0x703b52d2b0e9bb784623d9a0f3b3d7eda5be0967", "Balancer");
//        map.put("0xd4e45fbcb4991f1be1d09a71731ca5afcf106358", "Overnight Finance");
//        map.put("0x9e8487ea947a327596af1ef9eff6fc07fb8bfde9", "Beefy Finance");
//        map.put("0x03c20a2d9a0adf5bbf588c3673c201d99b320da0", "Yearn Finance");
//        map.put("0xf4f5256c15c380ed5fc25278fb36b5423192faec", "Curve Finance");
//
//        // NFT and Gaming protocols
//        map.put("0x91fd5a14a21dc9cf507546e6867d1fe8ee3bc2b9", "Base Punks");
//        map.put("0x2d95c7ec4c31d3ed534e9e40afd1901c962254b3", "Friend.tech");
//        map.put("0xb4fbf271143f4fbf7b91a5ded31805e42b2208d6", "Quests");
//        map.put("0xa902a7cc88a75192b4b09c92061d9d774a29f8bf", "Parallel");
//
//        // Oracles
//        map.put("0x71041dddad3595f9ced3dccfbe3d1f4b0a16bb70", "Chainlink");
//        map.put("0x3c4f787c29d5ae9d56e2ff134cf63bd9f523ff89", "Pyth Network");

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