package inpg.sn.lms.auth.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import inpg.sn.lms.auth.dto.RoleChangeRequest;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.service.RoleManagementService;
import inpg.sn.lms.auth.service.UserService;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserService userService;
    private final RoleManagementService roleManagementService;

    public AdminController(UserService userService, RoleManagementService roleManagementService) {
        this.userService = userService;
        this.roleManagementService = roleManagementService;
    }

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('ROLE_RESPONSABLE_ADMISSION')")
    public ResponseEntity<List<User>> listUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PutMapping("/users/{id}/roles")
    @PreAuthorize("hasAuthority('ROLE_RESPONSABLE_ADMISSION')")
    public ResponseEntity<?> changeUserRoles(@PathVariable Long id, @RequestBody RoleChangeRequest request) {
        return ResponseEntity.ok(roleManagementService.changerRole(id, request));
    }
}
