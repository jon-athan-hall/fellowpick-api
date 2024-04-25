package com.fellowpick.api.deck;

import jakarta.validation.Valid;
import org.apache.coyote.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

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

    @GetMapping("/{id}")
    public ResponseEntity<Deck> getDeck(@PathVariable Long id) {
        Optional<Deck> optionalDeck = deckService.getDeck(id);

        // More fancy way of returning the response.
        // return optionalDeck.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
         if (optionalDeck.isEmpty()) {
           return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
         }
         return ResponseEntity.ok(optionalDeck.get());
    }

    @PostMapping
    public ResponseEntity<Deck> createDeck(@Valid @RequestBody Deck deck) {
        try {
            Deck savedDeck = deckService.createDeck(deck);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedDeck);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

    }
}
