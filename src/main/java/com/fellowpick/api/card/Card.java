package com.fellowpick.api.card;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;

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

    /**
     * Constructors.
     */
    public Card() {}

    public Card(String id, String name) {
        this.id = id;
        this.name = name;
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
