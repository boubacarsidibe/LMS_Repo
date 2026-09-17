package inpg.sn.lms.auth.service;

import java.util.List;

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

    @Transactional
    public User changerRole(Long userId, RoleChangeRequest request) {
        String roleCode = roleUniqueDemandé(request);
        Role role = roleRepository.findByCode(roleCode);
        if (role == null) {
            throw new IllegalArgumentException("Le rôle demandé n'existe pas.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", userId));

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
