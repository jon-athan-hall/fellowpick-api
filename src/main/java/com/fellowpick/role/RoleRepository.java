package com.fellowpick.role;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// Data access layer for Role entities with name-based lookups.
public interface RoleRepository extends JpaRepository<Role, String> {

    // Finds a role by its unique name.
    Optional<Role> findByName(String name);

    // Checks whether a role with the given name already exists.
    boolean existsByName(String name);
}
