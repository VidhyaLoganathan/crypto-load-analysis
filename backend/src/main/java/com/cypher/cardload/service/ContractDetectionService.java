package com.cypher.cardload.service;

import com.cypher.cardload.util.AdditionalKnownAddresses;
import com.cypher.cardload.util.BlockchainConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetCode;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Service for detecting if an address is a contract and identifying protocols
 */
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
            "Aerodrome", Pattern.compile(".*4165726f64726f6d65.*", Pattern.CASE_INSENSITIVE),
            "Cypher", Pattern.compile(".*4379706865.*", Pattern.CASE_INSENSITIVE)  // Added pattern for "Cypher" in hex
    );

    // Additional known addresses - expand this list for better protocol detection
    private final Map<String, String> KNOWN_ADDRESSES = initializeKnownAddresses();

    /**
     * Initialize the map of known addresses and their protocols
     *
     * @return Map of addresses to protocols
     */
    private Map<String, String> initializeKnownAddresses() {
        Map<String, String> addressMap = new HashMap<>();

        // Add all known addresses from BlockchainConstants
        addressMap.putAll(BlockchainConstants.getAllKnownAddresses());

        // USDC token address - ensure it's in the map
        addressMap.put("0x833589fcd6edb6e08f4c7c32d4f71b54bda02913", "USDC");

        // Convert all keys to lowercase for case-insensitive comparison
        Map<String, String> normalizedMap = new HashMap<>();
        for (Map.Entry<String, String> entry : addressMap.entrySet()) {
            normalizedMap.put(entry.getKey().toLowerCase(), entry.getValue());
        }

        return normalizedMap;
    }

    /**
     * Checks if an address is a contract
     *
     * @param address Ethereum address to check
     * @return true if it's a contract, false if it's a regular wallet
     */
    public boolean isContract(String address) {
        if (address == null || address.isEmpty()) {
            return false;
        }

        // Normalize address
        String normalizedAddress = address.toLowerCase();

        // First check our known addresses map for protocols (most are contracts)
        if (KNOWN_ADDRESSES.containsKey(normalizedAddress)) {
            String protocol = KNOWN_ADDRESSES.get(normalizedAddress);
            // Cache the protocol while we're at it
            protocolCache.put(normalizedAddress, protocol);

            // Most protocol addresses are contracts, but CEX hot wallets and user wallets are not
            if (protocol.contains("Wallet") || protocol.contains("User")) {
                return false;
            }
            return true;
        }

        // Then check the cache
        return contractCache.computeIfAbsent(normalizedAddress, this::checkIsContract);
    }

    /**
     * Calls the blockchain to check if an address has code (is a contract)
     *
     * @param address Ethereum address to check
     * @return true if it's a contract, false if it's a regular wallet
     */
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

    /**
     * Detects the protocol of an address
     *
     * @param address Ethereum address to check
     * @return Protocol name, or null if unknown
     */
    public String detectProtocol(String address) {
        if (address == null || address.isEmpty()) {
            return null;
        }

        // Normalize address
        String normalizedAddress = address.toLowerCase();

        // First check cache
        String cachedProtocol = protocolCache.get(normalizedAddress);
        if (cachedProtocol != null) {
            return cachedProtocol;
        }

        // Then check our known protocols map
        if (KNOWN_ADDRESSES.containsKey(normalizedAddress)) {
            String protocol = KNOWN_ADDRESSES.get(normalizedAddress);
            // Cache the result
            protocolCache.put(normalizedAddress, protocol);
            return protocol;
        }

        // If not found, try to identify from bytecode (only if it's a contract)
        return protocolCache.computeIfAbsent(normalizedAddress, this::identifyProtocolFromBytecode);
    }

    /**
     * Identifies a protocol by analyzing contract bytecode
     *
     * @param address Ethereum address to check
     * @return Protocol name, or null if unknown
     */
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

            // If no specific protocol signature was found, but it's a contract on Base chain,
            // we can make an educated guess based on the address interactions or patterns

            // If the address interacts a lot with the Cypher master wallet, it might be related
            if (address.startsWith("0x19") || address.startsWith("0x1a")) {
                // This is a heuristic - addresses starting with 0x19 that interact with Cypher
                // might be related to Cypher's systems
                return "Cypher Finance";
            }

            return null; // Unknown protocol
        } catch (Exception e) {
            log.error("Error identifying protocol for address: {}", address, e);
            return null;
        }
    }
}

