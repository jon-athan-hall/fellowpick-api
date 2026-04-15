package com.fellowpick.user;

import com.fellowpick.role.Role;
import com.fellowpick.user.dto.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// MapStruct mapper for converting User entities to response DTOs.
@Mapper(componentModel = "spring")
public interface UserMapper {

    // Converts a User entity to a UserResponse, flattening roles to name strings.
    @Mapping(target = "roles", expression = "java(mapRoles(user.getRoles()))")
    UserResponse toUserResponse(User user);

    // Converts a list of User entities to a list of UserResponse DTOs.
    List<UserResponse> toUserResponseList(List<User> users);

    // Extracts role names from Role entities into a set of strings.
    default Set<String> mapRoles(Set<Role> roles) {
        return roles.stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
    }
}
