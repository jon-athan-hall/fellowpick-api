package com.fellowpick.pick;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

// Data access layer for Pick entities with custom aggregation queries.
public interface PickRepository extends JpaRepository<Pick, String> {

    @Query("""
            SELECT p.cardId AS cardId, p.pickType AS pickType, COUNT(p) AS count
            FROM Pick p
            WHERE p.preconId = :preconId
            GROUP BY p.cardId, p.pickType
            """)
    // Counts picks per card and type for a given precon.
    List<PickCountProjection> countByPrecon(String preconId);

    // Finds all picks a specific user made in a specific precon.
    List<Pick> findByUserIdAndPreconId(String userId, String preconId);

    // Finds a pick by ID, scoped to the owning user.
    Optional<Pick> findByIdAndUserId(String id, String userId);

    // Checks whether a user already has an identical pick to prevent duplicates.
    boolean existsByUserIdAndPreconIdAndCardIdAndPickType(
            String userId, String preconId, String cardId, PickType pickType);
}
