package inpg.sn.lms.config;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import inpg.sn.lms.auth.dto.UserDto;
import inpg.sn.lms.auth.entity.Role;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.RoleRepository;
import inpg.sn.lms.auth.repository.UserRepository;
import inpg.sn.lms.auth.service.PasswordPolicy;
import inpg.sn.lms.auth.service.UserService;

/**
 * Comptes du personnel créés au premier démarrage, à partir de variables d'environnement.
 *
 * <p>Sur une base neuve en production, il n'existait <strong>aucun compte</strong> (les comptes de démonstration ne
 * sont créés qu'avec {@code APP_DEMO_DATA_ENABLED=true}) et personne ne pouvait en attribuer : il fallait insérer un
 * utilisateur et son mot de passe haché à la main (audit du 2026-10-14). Les rôles du personnel ne s'accordent pas
 * depuis l'application (voir {@code RoleManagementService}) : c'est ici qu'ils naissent.
 *
 * <p>Idempotent : un compte dont l'e-mail existe déjà n'est jamais modifié — son mot de passe n'est pas réinitialisé à
 * chaque démarrage. Le mot de passe initial est commun et doit être changé à la première connexion. Un e-mail renseigné
 * sans mot de passe valable arrête le démarrage plutôt que de créer un compte inutilisable ou faible.
 */
@Component
public class ComptesInitiaux implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ComptesInitiaux.class);

    private final UserService userService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final String motDePasse;
    private final Map<String, String> emailsParRole = new LinkedHashMap<>();

    public ComptesInitiaux(UserService userService, UserRepository userRepository, RoleRepository roleRepository,
            @Value("${app.comptes-initiaux.mot-de-passe:}") String motDePasse,
            @Value("${app.comptes-initiaux.admission:}") String admission,
            @Value("${app.comptes-initiaux.pedagogie:}") String pedagogie,
            @Value("${app.comptes-initiaux.evaluateur:}") String evaluateur) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.motDePasse = motDePasse;
        emailsParRole.put("ROLE_RESPONSABLE_ADMISSION", admission);
        emailsParRole.put("ROLE_RESPONSABLE_PEDAGOGIQUE", pedagogie);
        emailsParRole.put("ROLE_EVALUATEUR_ACCREDITATION", evaluateur);
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (Map.Entry<String, String> entree : emailsParRole.entrySet()) {
            String email = entree.getValue() == null ? "" : entree.getValue().trim().toLowerCase(Locale.ROOT);
            if (!email.isEmpty()) {
                creerSiAbsent(entree.getKey(), email);
            }
        }
    }

    private void creerSiAbsent(String codeRole, String email) {
        if (userRepository.findByEmail(email) != null) {
            return;
        }
        if (motDePasse == null || motDePasse.isBlank()) {
            throw new IllegalStateException("APP_INIT_PASSWORD est vide : impossible de créer le compte initial " + email
                    + ". Renseignez un mot de passe initial (à changer à la première connexion), ou retirez l'e-mail.");
        }
        try {
            PasswordPolicy.exigerMotDePasseRobuste(motDePasse);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("APP_INIT_PASSWORD refusé : " + e.getMessage(), e);
        }

        UserDto dto = new UserDto();
        dto.setEmail(email);
        dto.setUsername(email.substring(0, email.indexOf('@') > 0 ? email.indexOf('@') : email.length()));
        dto.setPassword(motDePasse);
        dto.setNom("");
        dto.setPrenom("");
        dto.setTelephone("");

        if ("ROLE_RESPONSABLE_ADMISSION".equals(codeRole)) {
            // Seul rôle du personnel qui a une fiche de profil (responsables_admission).
            userService.createResponsableAdmission(dto);
        } else {
            Role role = roleRepository.findByCode(codeRole);
            if (role == null) {
                throw new IllegalStateException("Rôle " + codeRole + " introuvable : les migrations ne sont pas passées.");
            }
            User user = userService.createUser(dto);
            user.getRoles().clear();
            user.addRole(role);
            userRepository.save(user);
        }
        log.warn("Compte initial créé : {} ({}). Changez son mot de passe dès la première connexion.", email, codeRole);
    }
}
