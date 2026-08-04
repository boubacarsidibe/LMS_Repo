package inpg.sn.lms.auth.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
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
@CrossOrigin(origins = "http://localhost:4200")
public class CrudControlleur {
    private final UserService userService;
    private final CandidatService candidatService;
    private final EtudiantService etudiantService;

    public CrudControlleur(UserService userService, CandidatService candidatService, EtudiantService etudiantService) {
        this.userService = userService;
        this.candidatService = candidatService;
        this.etudiantService = etudiantService;
    }

    @GetMapping("/test")
    public String test() {
        return "Hello from AuthController";
    }

    @PostMapping("/register")
    public User registerUser(@RequestBody UserDto userDto) {
        return userService.createUser(userDto);

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

    

    @PostMapping("/registerEtudiant")
    public String registerEtudiant(@RequestBody EtudiantDto etudiantDto) {
        etudiantService.createEtudiant(etudiantDto);
        return "Etudiant registered successfully";
    }

    @GetMapping("/getAllCandidats")
    public ResponseEntity<List<Candidat>> getCandidats() {
        return ResponseEntity.ok(candidatService.getAllCandidats());
    }
}
