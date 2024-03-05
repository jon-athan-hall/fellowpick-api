package com.fellowpick.api.card;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @ElementCollection
    @Enumerated(EnumType.STRING)
    private Set<Color> colorIdentity;

    @ManyToOne
    @JoinColumn(name = "deck_id", referencedColumnName = "id")
    @JsonIgnore
    private Deck deck;

    @OneToMany(mappedBy = "card", cascade = CascadeType.ALL)
    private Set<Pick> picks;

    /**
     * Constructors.
     */
    public Card() {}

    public Card(String id, String name, Set<Color> colorIdentity, Deck deck, Set<Pick> picks) {
        this.id = id;
        this.name = name;
        this.colorIdentity = colorIdentity;
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

    public Set<Color> getColorIdentity() { return colorIdentity; };

    public void setColorIdentity(Set<Color> colorIdentity) { this.colorIdentity = colorIdentity; }

    public Deck getDeck() { return deck; }

    public void setDeck(Deck deck) { this.deck = deck; }

    public Set<Pick> getPicks() { return picks; }

    public void setPicks(Set<Pick> picks) { this.picks = picks; }

    /**
     * Takes a set code and number and creates a proper id value with proper lead padding.
     */
    public static String createIdFromSetCodeAndNumber(String setCode, String number) {
        return setCode + String.format("%04d", Integer.parseInt(number));
    }
}
