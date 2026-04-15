package com.fellowpick.role;

import com.fellowpick.exception.RoleInUseException;
import com.fellowpick.exception.RoleNameAlreadyExistsException;
import com.fellowpick.role.dto.RoleRequest;
import com.fellowpick.role.dto.RoleResponse;
import com.fellowpick.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Unit tests for RoleService covering CRUD operations and conflict checks.
@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    private static final String ROLE_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final String OTHER_ROLE_ID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
    private static final String MISSING_ROLE_ID = "cccccccc-cccc-cccc-cccc-cccccccccccc";

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleMapper roleMapper;

    @InjectMocks
    private RoleService roleService;

    private Role testRole;
    private RoleResponse testRoleResponse;

    @BeforeEach
    void setUp() {
        testRole = new Role("ROLE_USER");
        testRole.setId(ROLE_ID);
        testRoleResponse = new RoleResponse(ROLE_ID, "ROLE_USER");
    }

    // Verifies that getAllRoles returns all roles mapped to responses.
    @Test
    void getAllRoles_shouldReturnList() {
        when(roleRepository.findAll()).thenReturn(List.of(testRole));
        when(roleMapper.toRoleResponseList(List.of(testRole))).thenReturn(List.of(testRoleResponse));

        List<RoleResponse> result = roleService.getAllRoles();

        assertEquals(1, result.size());
        assertEquals(testRoleResponse, result.getFirst());
    }

    // Verifies that getRoleById returns the role when it exists.
    @Test
    void getRoleById_whenExists_shouldReturn() {
        when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.of(testRole));
        when(roleMapper.toRoleResponse(testRole)).thenReturn(testRoleResponse);

        assertEquals(testRoleResponse, roleService.getRoleById(ROLE_ID));
    }

    // Verifies that getRoleById throws EntityNotFoundException when the role is missing.
    @Test
    void getRoleById_whenNotFound_shouldThrow() {
        when(roleRepository.findById(MISSING_ROLE_ID)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> roleService.getRoleById(MISSING_ROLE_ID));
    }

    // Verifies that createRole saves a new role and returns the mapped response.
    @Test
    void createRole_shouldSaveAndReturn() {
        RoleRequest request = new RoleRequest("ROLE_NEW");
        when(roleRepository.existsByName("ROLE_NEW")).thenReturn(false);
        when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));
        when(roleMapper.toRoleResponse(any(Role.class))).thenReturn(new RoleResponse(OTHER_ROLE_ID, "ROLE_NEW"));

        RoleResponse result = roleService.createRole(request);

        assertEquals("ROLE_NEW", result.name());
        verify(roleRepository).save(any(Role.class));
    }

    // Verifies that creating a role with an existing name throws RoleNameAlreadyExistsException.
    @Test
    void createRole_duplicateName_shouldThrow() {
        when(roleRepository.existsByName("ROLE_USER")).thenReturn(true);

        assertThrows(RoleNameAlreadyExistsException.class,
                () -> roleService.createRole(new RoleRequest("ROLE_USER")));
        verify(roleRepository, never()).save(any());
    }

    // Verifies that updateRole changes the role's name.
    @Test
    void updateRole_shouldUpdateName() {
        when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.of(testRole));
        when(roleRepository.existsByName("ROLE_RENAMED")).thenReturn(false);
        when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));
        when(roleMapper.toRoleResponse(any(Role.class))).thenReturn(new RoleResponse(ROLE_ID, "ROLE_RENAMED"));

        roleService.updateRole(ROLE_ID, new RoleRequest("ROLE_RENAMED"));

        assertEquals("ROLE_RENAMED", testRole.getName());
    }

    // Verifies that updating a role to the same name skips the duplicate name check.
    @Test
    void updateRole_sameName_shouldSkipDuplicateCheck() {
        when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.of(testRole));
        when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));
        when(roleMapper.toRoleResponse(any(Role.class))).thenReturn(testRoleResponse);

        roleService.updateRole(ROLE_ID, new RoleRequest("ROLE_USER"));

        verify(roleRepository, never()).existsByName(any());
    }

    // Verifies that renaming a role to an already-taken name throws RoleNameAlreadyExistsException.
    @Test
    void updateRole_collidingName_shouldThrow() {
        when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.of(testRole));
        when(roleRepository.existsByName("ROLE_ADMIN")).thenReturn(true);

        assertThrows(RoleNameAlreadyExistsException.class,
                () -> roleService.updateRole(ROLE_ID, new RoleRequest("ROLE_ADMIN")));
    }

    // Verifies that updating a nonexistent role throws EntityNotFoundException.
    @Test
    void updateRole_notFound_shouldThrow() {
        when(roleRepository.findById(MISSING_ROLE_ID)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> roleService.updateRole(MISSING_ROLE_ID, new RoleRequest("ROLE_X")));
    }

    // Verifies that an unused role can be deleted.
    @Test
    void deleteRole_whenUnused_shouldDelete() {
        when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.of(testRole));
        when(userRepository.existsByRolesId(ROLE_ID)).thenReturn(false);

        roleService.deleteRole(ROLE_ID);

        verify(roleRepository).delete(testRole);
    }

    // Verifies that deleting a role assigned to users throws RoleInUseException.
    @Test
    void deleteRole_whenInUse_shouldThrow() {
        when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.of(testRole));
        when(userRepository.existsByRolesId(ROLE_ID)).thenReturn(true);

        assertThrows(RoleInUseException.class, () -> roleService.deleteRole(ROLE_ID));
        verify(roleRepository, never()).delete(any(Role.class));
    }

    // Verifies that deleting a nonexistent role throws EntityNotFoundException.
    @Test
    void deleteRole_notFound_shouldThrow() {
        when(roleRepository.findById(MISSING_ROLE_ID)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> roleService.deleteRole(MISSING_ROLE_ID));
    }
}
