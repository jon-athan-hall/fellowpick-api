package com.fellowpick.api.deck;

import com.fellowpick.api.card.Card;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

/**
 * Represents domain model.
 */
@Entity
@Table(name = "deck")
public class Deck {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private String name;

    @OneToMany(mappedBy = "deck", cascade = CascadeType.ALL)
    @OrderBy("id ASC")
    private Set<Card> cards;

    /**
     * Constructors.
     */
    public Deck() {};

    public Deck(String name, Set<Card> cards) {
        this.name = name;
        this.cards = cards;
    }

    /**
     * Getters and setters.
     */
    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public Set<Card> getCards() { return cards; }

    public void setCards(Set<Card> cards) { this.cards = cards; }
}
