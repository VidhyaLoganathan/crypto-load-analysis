package com.cypher.cardload.util;
import java.util.Map;

public class BlockchainConstants {

    // Cypher Master Wallet
    public static final String CYPHER_MASTER_WALLET = "0xcCCd218A58B53C67fC17D8C87Cb90d83614e35fD";

    // DEX Routers on Base Chain
    public static final Map<String, String> DEX_ROUTERS = Map.of(
            "0x4cdf24e0584985c94879e85cd6ac5e25f31d5eca", "Aerodrome Router",
            "0xbbbc1f6be7b76a15b3532a2b27d26c8ca96eade8", "Uniswap V3 Router",
            "0xc30141b657f4216252dc59af2e7cdb9d8792e1b0", "Uniswap V3 Router 2"
    );

    // Common Base Chain Bridges
    public static final Map<String, String> BRIDGES = Map.of(
            "0xc5af84701f98fa483ece78af83f11b6c38aca71d", "Base Bridge",
            "0x3154cf16ccdb4c6d922629664174b904d80f2c35", "Base Bridge (L1)"
    );

    // Major CEX Hot Wallets
    public static final Map<String, String> CEX_WALLETS = Map.of(
            "0x28c6c06298d514db089934071355e5743bf21d60", "Binance Hot Wallet",
            "0xddfabcdc4d8ffc6d5beaf154f18b778f892a0740", "Coinbase Hot Wallet",
            "0x881d40237659c251811cec9c364ef91dc08d300c", "Kraken Hot Wallet"
    );

    // Common token addresses on Base chain
    public static final Map<String, String> TOKEN_ADDRESSES = Map.of(
            "0x833589fcd6edb6e08f4c7c32d4f71b54bda02913", "USDC",
            "0x50c5725949a6f0c72e6c4a641f24049a917db0cb", "DAI",
            "0x4200000000000000000000000000000000000006", "WETH",
            "0x4158734d47fc9692176b5085e0f52ee0da5d47f1", "USDT"
    );
}

