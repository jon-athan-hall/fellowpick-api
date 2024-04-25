package com.fellowpick.api.card;

import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CardService {
    private final CardRepository cardRepository;

    public CardService(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    public Optional<Card> getCard(String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException(("Card ID cannot be null or empty"));
        }
        return cardRepository.findById(id);
    }
}
