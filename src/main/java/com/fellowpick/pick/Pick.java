package com.fellowpick.pick;

import com.fellowpick.common.BaseEntity;
import com.fellowpick.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

// Represents a user's CUT or ADD vote for a specific card in a precon deck.
@Entity
@Table(
        name = "picks",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_picks_user_precon_card_type",
                columnNames = {"user_id", "precon_id", "card_id", "pick_type"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class Pick extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(length = 36, updatable = false, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "precon_id", nullable = false, length = 100)
    private String preconId;

    @Column(name = "card_id", nullable = false, length = 20)
    private String cardId;

    @Enumerated(EnumType.STRING)
    @Column(name = "pick_type", nullable = false, length = 3)
    private PickType pickType;
}
