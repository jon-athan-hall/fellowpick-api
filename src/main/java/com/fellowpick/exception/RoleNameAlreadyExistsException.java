package com.fellowpick.exception;

// Thrown when creating or renaming a role to a name that already exists.
public class RoleNameAlreadyExistsException extends RuntimeException {

    public RoleNameAlreadyExistsException(String name) {
        super("A role with name '" + name + "' already exists");
    }
}
