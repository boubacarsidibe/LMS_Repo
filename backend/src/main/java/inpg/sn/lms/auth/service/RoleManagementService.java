package inpg.sn.lms.auth.service;

import java.util.List;
import java.util.Set;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import inpg.sn.lms.auth.dto.RoleChangeRequest;
import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.auth.entity.Formateur;
import inpg.sn.lms.auth.entity.Role;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.EtudiantRepository;
import inpg.sn.lms.auth.repository.FormateurRepository;
import inpg.sn.lms.auth.repository.RoleRepository;
import inpg.sn.lms.auth.repository.UserRepository;
import inpg.sn.lms.exception.ResourceNotFoundException;

/** Applique un changement de rôle complet ou ne modifie pas le compte. */
@Service
public class RoleManagementService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final FormateurRepository formateurRepository;
    private final EtudiantRepository etudiantRepository;

    public RoleManagementService(UserRepository userRepository, RoleRepository roleRepository,
            FormateurRepository formateurRepository, EtudiantRepository etudiantRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.formateurRepository = formateurRepository;
        this.etudiantRepository = etudiantRepository;
    }

    /**
     * Rôles que le responsable admission peut attribuer depuis l'application.
     *
     * <p>La route acceptait tout rôle existant : un responsable admission pouvait faire de n'importe quel compte un
     * responsable pédagogique, un évaluateur ou un autre responsable admission, et retirer leur rôle aux autres
     * responsables (audit du 2026-10-12, C3, reproduit le 2026-10-14). Les rôles du personnel naissent désormais au
     * démarrage, par {@code ComptesInitiaux} ; l'alumni est promu par la pédagogie, l'école et l'entreprise par leur
     * propre dépôt.
     */
    static final Set<String> ROLES_ATTRIBUABLES = Set.of("ROLE_CANDIDAT", "ROLE_ETUDIANT", "ROLE_FORMATEUR");

    /** Comptes du personnel : leur rôle ne se modifie pas depuis l'application, le sien compris. */
    static final Set<String> ROLES_PROTEGES = Set.of(
            "ROLE_RESPONSABLE_ADMISSION", "ROLE_RESPONSABLE_PEDAGOGIQUE", "ROLE_EVALUATEUR_ACCREDITATION");

    @Transactional
    public User changerRole(Long userId, RoleChangeRequest request, String auteur) {
        String roleCode = roleUniqueDemandé(request);
        if (!ROLES_ATTRIBUABLES.contains(roleCode)) {
            throw new AccessDeniedException("Ce rôle ne s'attribue pas depuis l'application. Rôles possibles : "
                    + "candidat, étudiant, formateur.");
        }
        Role role = roleRepository.findByCode(roleCode);
        if (role == null) {
            throw new IllegalArgumentException("Le rôle demandé n'existe pas.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", userId));
        if (auteur != null && (auteur.equalsIgnoreCase(user.getUsername()) || auteur.equalsIgnoreCase(user.getEmail()))) {
            throw new AccessDeniedException("Vous ne pouvez pas modifier le rôle de votre propre compte.");
        }
        if (user.getRoles().stream().anyMatch(r -> ROLES_PROTEGES.contains(r.getCode()))) {
            throw new AccessDeniedException("Le rôle d'un compte du personnel (admission, pédagogie, évaluation) "
                    + "ne se modifie pas depuis l'application.");
        }

        user.getRoles().clear();
        user.addRole(role);

        if ("ROLE_FORMATEUR".equals(roleCode) && formateurRepository.findByUserId(user.getId()).isEmpty()) {
            formateurRepository.save(nouveauFormateur(user));
        }
        if ("ROLE_ETUDIANT".equals(roleCode) && etudiantRepository.findByUserId(user.getId()).isEmpty()) {
            etudiantRepository.save(nouvelEtudiant(user));
        }

        return userRepository.save(user);
    }

    private String roleUniqueDemandé(RoleChangeRequest request) {
        List<String> roles = request == null ? null : request.getRoles();
        if (roles == null || roles.size() != 1 || roles.get(0) == null || roles.get(0).isBlank()) {
            throw new IllegalArgumentException("Un unique rôle valide doit être fourni.");
        }
        return roles.get(0).trim();
    }

    private Formateur nouveauFormateur(User user) {
        String[] nomPrenom = deriverNomPrenom(user.getUsername());
        return new Formateur(user, nomPrenom[0], nomPrenom[1], telephoneProvisoire(user));
    }

    private Etudiant nouvelEtudiant(User user) {
        String[] nomPrenom = deriverNomPrenom(user.getUsername());
        return new Etudiant(user, nomPrenom[0], nomPrenom[1], telephoneProvisoire(user));
    }

    private String telephoneProvisoire(User user) {
        return "N/A-" + user.getId();
    }

    private String[] deriverNomPrenom(String username) {
        String propre = username != null ? username.replaceAll("\\d+$", "") : "utilisateur";
        if (propre.contains("_")) {
            String[] parts = propre.split("_", 2);
            return new String[] { parts[1], parts[0] };
        }
        return new String[] { propre, propre };
    }
}
