package com.cypher.cardload.config;

import java.util.Map;

public class Constants {
    //cypher wallet address
    public static final String MASTER_WALLET_ADDRESS = "0xcCCd218A58B53C67fC17D8C87Cb90d83614e35fD";

    //Alchemy API url with key
    public static final String ALCHEMY_API_URL = "https://base-mainnet.g.alchemy.com/v2/5cTrJMY4BHtVjgKNC31Wek_Knoh2Yn5R";

    public static final String ZERO_ADDRESS = "0x0000000000000000000000000000000000000000";

    // Common ERC-20 tokens on Base
    public static final Map<String, String> TOKEN_ADDRESSES = Map.of(
            "USDC", "0x833589fCD6eDb6E08f4c7C32D4f71b54bdA02913",
            "ETH", "0x0000000000000000000000000000000000000000", // Native ETH
            "WETH", "0x4200000000000000000000000000000000000006",
            "DAI", "0x50c5725949A6F0c72E6C4a641F24049A917DB0Cb",
            "USDT", "0x7c8dABe453A128639C60e268Acf9E9b4D699a4Bc"
    );

    // Aerodrome pool addresses for price computation
    public static final Map<String, String> AERODROME_POOL_ADDRESSES = Map.of(
            "USDC-ETH", "0xAE178c2Cf73C12673167646D9E37533cAFC1A1B4",
            "DAI-USDC", "0x6EAfBf58b251a8Bd32CA73c3DE3E4B95D124A6f7",
            "USDT-USDC", "0x5095b92B84b97Cc926c7D3B87CABdF18c1e4277d"
    );
}