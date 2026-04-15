package com.fellowpick.exception;

// Thrown when attempting to delete a role that is still assigned to users.
public class RoleInUseException extends RuntimeException {

    public RoleInUseException(String roleId) {
        super("Role with id " + roleId + " cannot be deleted because it is assigned to one or more users");
    }
}
