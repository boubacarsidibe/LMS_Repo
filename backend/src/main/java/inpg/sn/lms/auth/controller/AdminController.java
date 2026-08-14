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
import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.auth.entity.Formateur;
import inpg.sn.lms.auth.entity.Role;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.EtudiantRepository;
import inpg.sn.lms.auth.repository.FormateurRepository;
import inpg.sn.lms.auth.repository.RoleRepository;
import inpg.sn.lms.auth.repository.UserRepository;
import inpg.sn.lms.auth.service.UserService;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final FormateurRepository formateurRepository;
    private final EtudiantRepository etudiantRepository;

    public AdminController(UserService userService, UserRepository userRepository, RoleRepository roleRepository,
            FormateurRepository formateurRepository, EtudiantRepository etudiantRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.formateurRepository = formateurRepository;
        this.etudiantRepository = etudiantRepository;
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
        // Un compte ne peut détenir qu'un seul rôle à la fois (Candidat, Étudiant, Formateur
        // ou Responsable Admission) : on n'applique que le premier rôle valide demandé, même
        // si le client en envoie plusieurs.
        if (request != null && request.getRoles() != null) {
            for (String roleCode : request.getRoles()) {
                Role r = roleRepository.findByCode(roleCode);
                if (r != null) {
                    user.addRole(r);
                    break;
                }
            }
        }
        userRepository.save(user);

        // Attribuer un rôle FORMATEUR/ETUDIANT sans profil associé rend tout
        // l'espace pédagogique inutilisable (404 sur /formateur/profile, etc.),
        // car ces endpoints exigent une entité Formateur/Etudiant réelle, pas
        // seulement le rôle. On la crée donc immédiatement si elle manque.
        boolean estFormateur = user.getRoles().stream().anyMatch(r -> "ROLE_FORMATEUR".equals(r.getCode()));
        if (estFormateur && formateurRepository.findByUserId(user.getId()).isEmpty()) {
            formateurRepository.save(nouveauFormateur(user));
        }
        boolean estEtudiant = user.getRoles().stream().anyMatch(r -> "ROLE_ETUDIANT".equals(r.getCode()));
        if (estEtudiant && etudiantRepository.findByUserId(user.getId()).isEmpty()) {
            etudiantRepository.save(nouvelEtudiant(user));
        }

        return ResponseEntity.ok(user);
    }

    private Formateur nouveauFormateur(User user) {
        String[] nomPrenom = deriverNomPrenom(user.getUsername());
        // "N/A" seul violerait la contrainte unique sur telephone dès le 2e profil sans
        // numéro : on le rend unique par utilisateur, comme AdminRecrutementController#inscrireCommeEtudiant.
        return new Formateur(user, nomPrenom[0], nomPrenom[1], "N/A-" + user.getId());
    }

    private Etudiant nouvelEtudiant(User user) {
        String[] nomPrenom = deriverNomPrenom(user.getUsername());
        return new Etudiant(user, nomPrenom[0], nomPrenom[1], "N/A-" + user.getId());
    }
    

    /** Dérive un [nom, prenom] provisoire depuis le username (ex: "amina_diop" -> ["diop", "amina"]), à corriger par l'intéressé depuis son profil. */
    private String[] deriverNomPrenom(String username) {
        String propre = username != null ? username.replaceAll("\\d+$", "") : "utilisateur";
        if (propre.contains("_")) {
            String[] parts = propre.split("_", 2);
            return new String[] { parts[1], parts[0] };
        }
        return new String[] { propre, propre };
    }
}
