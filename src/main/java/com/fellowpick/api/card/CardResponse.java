package com.fellowpick.api.card;

import com.fellowpick.api.deck.Deck;
import com.fellowpick.api.pick.Pick;

import java.util.Set;

public class CardResponse {
    private String id;
    private String name;

    public CardResponse(Card card) {
        this.id = card.getId();
        this.name = card.getName();
    }

    /**
     * Getters and setters.
     */
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
