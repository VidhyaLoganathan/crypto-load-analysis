package com.cypher.cardload.repository;

import com.cypher.cardload.model.CounterpartyInfo;
import com.cypher.cardload.model.CounterpartyType;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class CounterpartyRepositoryImpl implements CounterpartyRepository {
    private final Map<String, CounterpartyInfo> knownEntities = new HashMap<>();

    @PostConstruct
    public void initialize() {
        // Initialize with some known entities - in production, this would come from a database

        // Exchanges
        addKnownEntity("0x28c6c06298d514db089934071355e5743bf21d60", "Binance", CounterpartyType.EXCHANGE, "Binance");
        addKnownEntity("0xddfabcdc4d8ffc6d5beaf154f18b778f892a0740", "Coinbase", CounterpartyType.EXCHANGE, "Coinbase");
        addKnownEntity("0x881d40237659c251811cec9c364ef91dc08d300c", "Kraken", CounterpartyType.EXCHANGE, "Kraken");

        // Protocols
        addKnownEntity("0x4cdf24e0584985c94879e85cd6ac5e25f31d5eca", "Aerodrome Router", CounterpartyType.PROTOCOL, "Aerodrome Finance");
        addKnownEntity("0xc5af84701f98fa483ece78af83f11b6c38aca71d", "Base Bridge", CounterpartyType.PROTOCOL, "Base Bridge");
        addKnownEntity("0xbbbc1f6be7b76a15b3532a2b27d26c8ca96eade8", "Uniswap V3 Router", CounterpartyType.PROTOCOL, "Uniswap");
        addKnownEntity("0xc30141b657f4216252dc59af2e7cdb9d8792e1b0", "Swap Router", CounterpartyType.PROTOCOL, "Uniswap");

        // Major wallets
        addKnownEntity("0xcCCd218A58B53C67fC17D8C87Cb90d83614e35fD", "Cypher Master Wallet", CounterpartyType.WALLET, null);
    }

    private void addKnownEntity(String address, String name, CounterpartyType type, String protocol) {
        CounterpartyInfo entity = CounterpartyInfo.builder()
                .address(address.toLowerCase())
                .name(name)
                .type(type)
                .protocol(protocol)
                .isKnownEntity(true)
                .build();
        knownEntities.put(address.toLowerCase(), entity);
    }

    @Override
    public Optional<CounterpartyInfo> findByAddress(String address) {
        return Optional.ofNullable(knownEntities.get(address.toLowerCase()));
    }

    @Override
    public List<CounterpartyInfo> findKnownEntities() {
        return knownEntities.values().stream().collect(Collectors.toList());
    }
}


