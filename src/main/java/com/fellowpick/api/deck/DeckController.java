package com.fellowpick.api.deck;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Handles HTTP requests.
 */
@RestController
@RequestMapping("/decks")
public class DeckController {
    private final DeckService deckService;

    public DeckController(DeckService deckService) {
        this.deckService = deckService;
    }

    @GetMapping
    public ResponseEntity<List<Deck>> getAllDecks() {
        List<Deck> decks = deckService.getAllDecks();
        return ResponseEntity.ok(decks);
    }

    @PostMapping
    public ResponseEntity<Deck> createDeck(@RequestBody Deck deck) {
        Deck savedDeck = deckService.createDeck(deck);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedDeck);
    }
}
