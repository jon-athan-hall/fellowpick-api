package com.fellowpick.role;

import com.fellowpick.role.dto.RoleResponse;
import org.mapstruct.Mapper;

import java.util.List;

// MapStruct mapper for converting Role entities to response DTOs.
@Mapper(componentModel = "spring")
public interface RoleMapper {

    // Converts a single Role entity to a RoleResponse DTO.
    RoleResponse toRoleResponse(Role role);

    // Converts a list of Role entities to a list of RoleResponse DTOs.
    List<RoleResponse> toRoleResponseList(List<Role> roles);
}
