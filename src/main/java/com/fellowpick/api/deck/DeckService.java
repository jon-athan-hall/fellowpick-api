package com.fellowpick.api.deck;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Encapsulates business logic.
 */
@Service
public class DeckService {
    private final DeckRepository deckRepository;

    public DeckService(DeckRepository deckRepository) {
        this.deckRepository = deckRepository;
    }

    public List<Deck> getAllDecks() {
        // Add business logic or validation here before returning.
        return deckRepository.findAll();
    }

    public Optional<Deck> getDeck(Long id) {
        return deckRepository.findById(id);
    }

    public Deck createDeck(Deck deck) {
        // Add business logic or validation here before saving.
        return deckRepository.save(deck);
    }
}
