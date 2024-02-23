package com.fellowpick.api.deck;

import org.springframework.stereotype.Service;

import java.util.List;

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

    public Deck createDeck(Deck deck) {
        // Add business logic or validation here before saving.
        return deckRepository.save(deck);
    }
}
