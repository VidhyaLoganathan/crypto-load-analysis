package com.cypher.cardload.repository;

import com.cypher.cardload.model.TokenTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for persisting and querying TokenTransfer entities.
 */
public interface TokenTransferRepository
        extends JpaRepository<TokenTransfer, String> {

    /**
     * Finds all transfers whose timestamp is between the given start (inclusive)
     * and end (exclusive) datetimes.
     *
     * @param start the lower bound of the timestamp (inclusive)
     * @param end   the upper bound of the timestamp (exclusive)
     * @return a list of TokenTransfer within the specified time range
     */
    List<TokenTransfer> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    Optional<TokenTransfer> findTopByOrderByTimestampDesc();

    boolean existsByTimestampBetween(LocalDateTime start, LocalDateTime end);
}
