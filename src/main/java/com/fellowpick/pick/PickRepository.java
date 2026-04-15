package com.fellowpick.pick;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PickRepository extends JpaRepository<Pick, String> {

    @Query("""
            SELECT p.cardId AS cardId, p.pickType AS pickType, COUNT(p) AS count
            FROM Pick p
            WHERE p.preconId = :preconId
            GROUP BY p.cardId, p.pickType
            """)
    List<PickCountProjection> countByPrecon(String preconId);

    List<Pick> findByUserIdAndPreconId(String userId, String preconId);

    Optional<Pick> findByIdAndUserId(String id, String userId);

    boolean existsByUserIdAndPreconIdAndCardIdAndPickType(
            String userId, String preconId, String cardId, PickType pickType);
}
