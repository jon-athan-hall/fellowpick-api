package com.fellowpick.api.user;

import com.fellowpick.api.pick.Pick;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;

import java.util.Set;

@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Email
    private String email;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private Set<Pick> picks;

    /**
     * Constructors.
     */
    public User() {}

    public User(String email) {
        this.email = email;
    }

    /**
     * Getters and setters.
     */
    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }

    public void setEmail(String email) { this.email = email; }

    public Set<Pick> getPicks() { return picks; }

    public void setPicks(Set<Pick> picks) { this.picks = picks; }
}
