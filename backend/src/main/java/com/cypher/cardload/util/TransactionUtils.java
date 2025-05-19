package com.cypher.cardload.util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.protocol.core.methods.response.Transaction;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class TransactionUtils {

    /**
     * Analyzes a transaction to see if it's a token transfer and extract recipient
     * Used for cases where the token contract is the 'to' address but the actual
     * recipient is in the transaction data
     */
    public Optional<String> extractTokenTransferRecipient(Transaction tx) {
        if (tx == null || tx.getInput() == null || tx.getInput().length() < 10) {
            return Optional.empty();
        }

        // Check for ERC20 transfer method signature: 0xa9059cbb
        String methodSignature = tx.getInput().substring(0, 10);
        if (!"0xa9059cbb".equals(methodSignature)) {
            return Optional.empty();
        }

        try {
            // Parse the method parameters (address _to, uint256 _value)
            Function function = new Function(
                    "transfer",
                    Collections.emptyList(),
                    Arrays.asList(new TypeReference<Address>() {}, new TypeReference<Type>() {})
            );

            List<Type> parameters = FunctionReturnDecoder.decode(
                    tx.getInput().substring(10),
                    function.getOutputParameters()
            );

            if (parameters != null && !parameters.isEmpty()) {
                return Optional.of(parameters.get(0).toString());
            }
        } catch (Exception e) {
            log.debug("Error extracting token transfer recipient: {}", e.getMessage());
        }

        return Optional.empty();
    }

    /**
     * Detects if a transaction is a token approval
     */
    public boolean isTokenApproval(Transaction tx) {
        if (tx == null || tx.getInput() == null || tx.getInput().length() < 10) {
            return false;
        }

        // Check for ERC20 approve method signature: 0x095ea7b3
        String methodSignature = tx.getInput().substring(0, 10);
        return "0x095ea7b3".equals(methodSignature);
    }

    /**
     * Extracts the spender from a token approval transaction
     */
    public Optional<String> extractApprovalSpender(Transaction tx) {
        if (!isTokenApproval(tx)) {
            return Optional.empty();
        }

        try {
            // Parse the method parameters (address _spender, uint256 _value)
            Function function = new Function(
                    "approve",
                    Collections.emptyList(),
                    Arrays.asList(new TypeReference<Address>() {}, new TypeReference<Type>() {})
            );

            List<Type> parameters = FunctionReturnDecoder.decode(
                    tx.getInput().substring(10),
                    function.getOutputParameters()
            );

            if (parameters != null && !parameters.isEmpty()) {
                return Optional.of(parameters.get(0).toString());
            }
        } catch (Exception e) {
            log.debug("Error extracting approval spender: {}", e.getMessage());
        }

        return Optional.empty();
    }

    /**
     * Formats blockchain addresses for display - shortens and adds ellipsis
     */
    public String formatAddress(String address) {
        if (address == null || address.length() < 10) {
            return address;
        }

        // Format as 0x1234...5678
        return address.substring(0, 6) + "..." + address.substring(address.length() - 4);
    }

    /**
     * Generates an etherscan URL for the given address
     */
    public String getEtherscanUrl(String address) {
        // Using Base Explorer URL
        return "https://basescan.org/address/" + address;
    }
}