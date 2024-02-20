package com.fellowpick.api.card;

import com.fellowpick.api.deck.Deck;
import com.fellowpick.api.pick.Pick;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

@Entity
public class Card {
    /**
     * There is no GeneratedValue strategy because the card
     * id will be supplied in the creation payload.
     */
    @Id
    private String id;

    @NotNull
    private String name;

    @ManyToOne
    @JoinColumn(name = "deck_id", referencedColumnName = "id")
    private Deck deck;

    @OneToMany(mappedBy = "pick", cascade = CascadeType.ALL)
    private Set<Pick> picks;

    /**
     * Constructors.
     */
    public Card() {}

    public Card(String id, String name, Deck deck, Set<Pick> picks) {
        this.id = id;
        this.name = name;
        this.deck = deck;
        this.picks = picks;
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

    public Deck getDeck() { return deck; }

    public void setDeck(Deck deck) { this.deck = deck; }

    public Set<Pick> getPicks() { return picks; }

    public void setPicks(Set<Pick> picks) { this.picks = picks; }
}
