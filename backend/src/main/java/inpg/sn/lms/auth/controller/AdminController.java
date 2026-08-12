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
import inpg.sn.lms.auth.entity.Role;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.RoleRepository;
import inpg.sn.lms.auth.repository.UserRepository;
import inpg.sn.lms.auth.service.UserService;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    public AdminController(UserService userService, UserRepository userRepository, RoleRepository roleRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('ROLE_RESPONSABLE_ADMISSION')")
    public ResponseEntity<List<User>> listUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PutMapping("/users/{id}/roles")
    @PreAuthorize("hasAuthority('ROLE_RESPONSABLE_ADMISSION')")
    public ResponseEntity<?> changeUserRoles(@PathVariable Long id, @RequestBody RoleChangeRequest request) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();

        user.getRoles().clear();
        if (request != null && request.getRoles() != null) {
            for (String roleCode : request.getRoles()) {
                Role r = roleRepository.findByCode(roleCode);
                if (r != null) user.addRole(r);
            }
        }
        userRepository.save(user);
        return ResponseEntity.ok(user);
    }
}
