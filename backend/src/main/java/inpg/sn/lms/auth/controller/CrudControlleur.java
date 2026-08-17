package inpg.sn.lms.auth.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import inpg.sn.lms.auth.dto.CandidatDto;
import inpg.sn.lms.auth.dto.EtudiantDto;
import inpg.sn.lms.auth.dto.UserDto;
import inpg.sn.lms.auth.entity.Candidat;

import inpg.sn.lms.auth.entity.User;

import inpg.sn.lms.auth.service.CandidatService;
import inpg.sn.lms.auth.service.EtudiantService;
import inpg.sn.lms.auth.service.UserService;

@RestController
@RequestMapping("api/auth")
public class CrudControlleur {
    private final UserService userService;
    private final inpg.sn.lms.security.JwtUtils jwtUtils;
    private final CandidatService candidatService;
    private final EtudiantService etudiantService;

    public CrudControlleur(UserService userService, CandidatService candidatService, EtudiantService etudiantService, inpg.sn.lms.security.JwtUtils jwtUtils) {
        this.userService = userService;
        this.candidatService = candidatService;
        this.etudiantService = etudiantService;
        this.jwtUtils = jwtUtils;
    }

    @GetMapping("/test")
    public String test() {
        return "Hello from AuthController";
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody UserDto userDto) {
        try {
            inpg.sn.lms.auth.entity.User newUser = userService.createUser(userDto);
            var userDetails = userService.loadUserByUsername(newUser.getUsername());
            String token = jwtUtils.generateToken(userDetails);
            Map<String, Object> user = Map.of(
                    "id", newUser.getId(),
                    "username", newUser.getUsername(),
                    "email", newUser.getEmail(),
                    "roles", newUser.getRoles().stream().map(role -> role.getCode()).toList());
            return ResponseEntity.ok(Map.of("token", token, "user", user));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("user")
    public ResponseEntity<User> getUserByEmail(@RequestParam String email) {
        User user = userService.findByEmail(email);
        if (user != null) {
            return ResponseEntity.ok(user);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/getAllUsers")
    public ResponseEntity<List<User>> getUsers() {
        // Remplacer userService.get() par userService.getAllUsers()
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PostMapping("/registerCandidat")
    public Candidat registerCandidat(@RequestBody CandidatDto candidatDto) {
        return candidatService.createCandidat(candidatDto);
    }

    


    @GetMapping("/getAllCandidats")
    public ResponseEntity<List<Candidat>> getCandidats() {
        return ResponseEntity.ok(candidatService.getAllCandidats());
    }
}
