package com.cypher.cardload.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetCode;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContractDetectionService {
    private final Web3j web3j;

    // Cache results to minimize blockchain calls
    private final Map<String, Boolean> contractCache = new ConcurrentHashMap<>();
    private final Map<String, String> protocolCache = new ConcurrentHashMap<>();

    // Protocol signatures - bytecode patterns that help identify common protocols
    private static final Map<String, Pattern> PROTOCOL_SIGNATURES = Map.of(
            "Uniswap", Pattern.compile("0x363d3d373d3d3d363d73.*5af43d82803e903d91602b57fd5bf3"),
            "Aave", Pattern.compile(".*41617665.*", Pattern.CASE_INSENSITIVE),
            "Compound", Pattern.compile(".*436f6d706f756e64.*", Pattern.CASE_INSENSITIVE),
            "Aerodrome", Pattern.compile(".*4165726f64726f6d65.*", Pattern.CASE_INSENSITIVE)
    );

    // Known contracts mapped to their protocols
    private static final Map<String, String> KNOWN_PROTOCOLS = Map.of(
            "0x4cdf24e0584985c94879e85cd6ac5e25f31d5eca", "Aerodrome Finance",
            "0xc5af84701f98fa483ece78af83f11b6c38aca71d", "Base Bridge",
            "0xbbbc1f6be7b76a15b3532a2b27d26c8ca96eade8", "Uniswap V3"
            // Add more known contracts here
    );

    // Known centralized exchange wallets
    private static final Map<String, String> KNOWN_EXCHANGES = Map.of(
            "0x28c6c06298d514db089934071355e5743bf21d60", "Binance",
            "0xddfabcdc4d8ffc6d5beaf154f18b778f892a0740", "Coinbase"
            // Add more exchange wallets here
    );

    public boolean isContract(String address) {
        return contractCache.computeIfAbsent(address, this::checkIsContract);
    }

    private boolean checkIsContract(String address) {
        try {
            EthGetCode ethGetCode = web3j.ethGetCode(address, DefaultBlockParameterName.LATEST).send();
            // If code length > 2 (0x), it's a contract
            return ethGetCode.getCode().length() > 2;
        } catch (IOException e) {
            log.error("Error checking if address is a contract: {}", address, e);
            return false;
        }
    }

    public String detectProtocol(String address) {
        // First check our mapped protocols
        if (KNOWN_PROTOCOLS.containsKey(address.toLowerCase())) {
            return KNOWN_PROTOCOLS.get(address.toLowerCase());
        }

        // Check if it's a known exchange
        if (KNOWN_EXCHANGES.containsKey(address.toLowerCase())) {
            return KNOWN_EXCHANGES.get(address.toLowerCase());
        }

        // Then check cache
        return protocolCache.computeIfAbsent(address, this::identifyProtocolFromBytecode);
    }

    private String identifyProtocolFromBytecode(String address) {
        try {
            if (!isContract(address)) {
                return null;
            }

            EthGetCode ethGetCode = web3j.ethGetCode(address, DefaultBlockParameterName.LATEST).send();
            String bytecode = ethGetCode.getCode();

            // Check for protocol signatures in the bytecode
            for (Map.Entry<String, Pattern> entry : PROTOCOL_SIGNATURES.entrySet()) {
                if (entry.getValue().matcher(bytecode).find()) {
                    return entry.getKey();
                }
            }

            return null; // Unknown protocol
        } catch (Exception e) {
            log.error("Error identifying protocol for address: {}", address, e);
            return null;
        }
    }
}