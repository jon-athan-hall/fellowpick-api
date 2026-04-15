CREATE TABLE picks (
    id          VARCHAR(36)  NOT NULL PRIMARY KEY,
    user_id     VARCHAR(36)  NOT NULL,
    precon_id   VARCHAR(100) NOT NULL,
    card_id     VARCHAR(20)  NOT NULL,
    pick_type   VARCHAR(3)   NOT NULL,
    created_at  DATETIME(6)  NOT NULL,
    created_by  VARCHAR(36),
    updated_at  DATETIME(6)  NOT NULL,
    updated_by  VARCHAR(36),

    CONSTRAINT fk_picks_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uk_picks_user_precon_card_type UNIQUE (user_id, precon_id, card_id, pick_type)
);

CREATE INDEX idx_picks_precon ON picks (precon_id);
CREATE INDEX idx_picks_user   ON picks (user_id);
