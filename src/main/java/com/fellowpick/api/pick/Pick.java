package com.fellowpick.api.pick;

import com.fellowpick.api.card.Card;
import com.fellowpick.api.user.User;
import jakarta.persistence.*;

@Entity(name = "pick")
public class Pick {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "card_id", referencedColumnName = "id")
    private Card card;

    @Enumerated(EnumType.STRING)
    private PickType pickType;
}
