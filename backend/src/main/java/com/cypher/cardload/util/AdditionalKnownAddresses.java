package com.cypher.cardload.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Additional known addresses for specific protocols on Base chain
 */
public class AdditionalKnownAddresses {

    /**
     * Get additional known addresses that might not be in the main BlockchainConstants
     * This is useful for updating address information without modifying the main constants
     *
     * @return Map of addresses to protocol names
     */
    public static Map<String, String> getAddresses() {
        Map<String, String> addresses = new HashMap<>();

        // Add the specific address that was missing a protocol
        addresses.put("0x19ceead7105607cd444f5ad10dd51356436095a1", "Cypher Finance");

        // Add other Base chain contracts that might be relevant
        addresses.put("0x06b74fe8070c96d92e3a2a8a871849ac81e4c09e", "Cypher User"); // This appears to be a frequent user

        // Add any other addresses that should have protocols but don't
        // This might require some research to identify what these contracts are

        return addresses;
    }
}
