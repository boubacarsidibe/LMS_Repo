package inpg.sn.lms.espace_etudiant.service;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import inpg.sn.lms.auth.entity.AlumniProfil;
import inpg.sn.lms.auth.entity.EcoleFormation;
import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.auth.entity.Formateur;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.AlumniProfilRepository;
import inpg.sn.lms.auth.repository.EcoleFormationRepository;
import inpg.sn.lms.auth.repository.EtudiantRepository;
import inpg.sn.lms.auth.repository.FormateurRepository;
import inpg.sn.lms.auth.repository.UserRepository;

/**
 * Résolution de l'utilisateur à l'origine de la requête.
 *
 * <p>Cette logique était recopiée à l'identique dans plusieurs contrôleurs ; elle
 * est centralisée ici pour que les nouveaux endpoints ne la réinventent pas.
 * Le repli sur l'e-mail est nécessaire car le sujet du jeton JWT est le
 * {@code username}, alors que la connexion s'effectue par e-mail.
 */
@Service
public class UtilisateurCourantService {

    private final UserRepository userRepository;
    private final EtudiantRepository etudiantRepository;
    private final FormateurRepository formateurRepository;
    private final AlumniProfilRepository alumniProfilRepository;
    private final EcoleFormationRepository ecoleFormationRepository;

    public UtilisateurCourantService(UserRepository userRepository, EtudiantRepository etudiantRepository,
            FormateurRepository formateurRepository, AlumniProfilRepository alumniProfilRepository,
            EcoleFormationRepository ecoleFormationRepository) {
        this.userRepository = userRepository;
        this.etudiantRepository = etudiantRepository;
        this.formateurRepository = formateurRepository;
        this.alumniProfilRepository = alumniProfilRepository;
        this.ecoleFormationRepository = ecoleFormationRepository;
    }

    /** Utilisateur connecté, ou {@code null} si la requête est anonyme. */
    public User utilisateur(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        String identifiant = authentication.getName();
        User user = userRepository.findByUsername(identifiant);
        return user != null ? user : userRepository.findByEmail(identifiant);
    }

    /** Profil étudiant du connecté, ou {@code null} s'il n'en a pas. */
    public Etudiant etudiant(Authentication authentication) {
        User user = utilisateur(authentication);
        return user == null ? null : etudiantRepository.findByUserId(user.getId()).orElse(null);
    }

    /** Profil formateur du connecté, ou {@code null} s'il n'en a pas. */
    public Formateur formateur(Authentication authentication) {
        User user = utilisateur(authentication);
        return user == null ? null : formateurRepository.findByUserId(user.getId()).orElse(null);
    }

    /** Profil alumni du connecté, ou {@code null} s'il n'en a pas. */
    public AlumniProfil alumniProfil(Authentication authentication) {
        User user = utilisateur(authentication);
        return user == null ? null : alumniProfilRepository.findByUserId(user.getId()).orElse(null);
    }

    /** Profil école de formation du connecté, ou {@code null} s'il n'en a pas. */
    public EcoleFormation ecoleFormation(Authentication authentication) {
        User user = utilisateur(authentication);
        return user == null ? null : ecoleFormationRepository.findByUserId(user.getId()).orElse(null);
    }

    /** Libellé « Prénom Nom » d'un utilisateur, avec repli sur son identifiant. */
    public String nomAffiche(User user) {
        if (user == null) {
            return "Utilisateur supprimé";
        }
        Etudiant etudiant = etudiantRepository.findByUserId(user.getId()).orElse(null);
        if (etudiant != null) {
            return etudiant.getPrenom() + " " + etudiant.getNom();
        }
        Formateur formateur = formateurRepository.findByUserId(user.getId()).orElse(null);
        if (formateur != null) {
            return formateur.getPrenom() + " " + formateur.getNom();
        }
        return user.getUsername();
    }
}
