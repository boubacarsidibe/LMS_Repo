package inpg.sn.lms.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import inpg.sn.lms.auth.dto.FormateurDto;

import inpg.sn.lms.auth.entity.Formateur;
import inpg.sn.lms.auth.entity.User;

import inpg.sn.lms.auth.repository.FormateurRepository;
import inpg.sn.lms.auth.repository.RoleRepository;

@Service
public class FormateurService {

    private final FormateurRepository formateurRepository;
    private final RoleRepository roleService;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;

    public FormateurService(FormateurRepository formateurRepository, RoleRepository roleService,
            PasswordEncoder passwordEncoder, UserService userService) {
        this.formateurRepository = formateurRepository;
        this.roleService = roleService;
        this.passwordEncoder = passwordEncoder;
        this.userService = userService;
    }

    public Iterable<Formateur> getAllFormateurs() {
        return formateurRepository.findAll();
    }

    /**
     * Crée un compte formateur.
     *
     * <p>Le compte doit être neuf : réutiliser un utilisateur existant reviendrait à
     * greffer le rôle FORMATEUR — et donc l'accès à tous les dossiers étudiants — sur
     * le compte d'un tiers, sans que celui-ci en sache rien et sans connaître son mot
     * de passe. Pour promouvoir un compte existant, passer par la gestion des rôles
     * ({@code PUT /api/admin/users/{id}/roles}).
     */
    public Formateur createFormateur(FormateurDto formateurDto) {
        if (userService.findByUsername(formateurDto.getUsername()) != null) {
            throw new IllegalStateException(
                    "Ce nom d'utilisateur est déjà pris. Pour donner le rôle formateur à un compte existant, utilisez la gestion des rôles.");
        }
        if (formateurDto.getEmail() != null && userService.findByEmail(formateurDto.getEmail()) != null) {
            throw new IllegalStateException(
                    "Cet e-mail est déjà associé à un compte. Pour donner le rôle formateur à un compte existant, utilisez la gestion des rôles.");
        }
        User user = new User(formateurDto.getUsername(), passwordEncoder.encode(formateurDto.getPassword()),
                formateurDto.getEmail());
        var roleFormateur = roleService.findByCode("ROLE_FORMATEUR");
        if (roleFormateur == null) {
            throw new IllegalStateException("Le rôle ROLE_FORMATEUR est introuvable.");
        }
        user.addRole(roleFormateur);
        user = userService.save(user);
        Formateur newFormateur = new Formateur(user, formateurDto.getNom(), formateurDto.getPrenom(),
                formateurDto.getTelephone());

        return formateurRepository.save(newFormateur);
    }
}
