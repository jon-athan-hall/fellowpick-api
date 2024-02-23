package com.fellowpick.api.deck;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Interacts with database.
 */
@Repository
public interface DeckRepository extends JpaRepository<Deck, Long> {
}
