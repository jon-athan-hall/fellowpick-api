package com.fellowpick.role;

import com.fellowpick.exception.RoleInUseException;
import com.fellowpick.exception.RoleNameAlreadyExistsException;
import com.fellowpick.role.dto.RoleRequest;
import com.fellowpick.role.dto.RoleResponse;
import com.fellowpick.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Business logic for creating, updating, listing, and deleting roles.
@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final RoleMapper roleMapper;

    public RoleService(RoleRepository roleRepository,
                       UserRepository userRepository,
                       RoleMapper roleMapper) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.roleMapper = roleMapper;
    }

    // Returns all roles in the system.
    public List<RoleResponse> getAllRoles() {
        return roleMapper.toRoleResponseList(roleRepository.findAll());
    }

    // Looks up a role by ID, throwing if not found.
    public RoleResponse getRoleById(String id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Role not found with id: " + id));
        return roleMapper.toRoleResponse(role);
    }

    // Creates a new role after verifying the name is unique.
    @Transactional
    public RoleResponse createRole(RoleRequest request) {
        if (roleRepository.existsByName(request.name())) {
            throw new RoleNameAlreadyExistsException(request.name());
        }
        Role role = new Role(request.name());
        return roleMapper.toRoleResponse(roleRepository.save(role));
    }

    // Renames a role, checking for name conflicts.
    @Transactional
    public RoleResponse updateRole(String id, RoleRequest request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Role not found with id: " + id));

        if (!role.getName().equals(request.name()) && roleRepository.existsByName(request.name())) {
            throw new RoleNameAlreadyExistsException(request.name());
        }

        role.setName(request.name());
        return roleMapper.toRoleResponse(roleRepository.save(role));
    }

    // Deletes a role, blocking if any users still hold it.
    @Transactional
    public void deleteRole(String id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Role not found with id: " + id));

        if (userRepository.existsByRolesId(id)) {
            throw new RoleInUseException(id);
        }

        roleRepository.delete(role);
    }
}
