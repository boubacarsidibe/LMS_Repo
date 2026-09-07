package inpg.sn.lms.espace_etudiant.service;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import inpg.sn.lms.auth.entity.AlumniProfil;
import inpg.sn.lms.auth.entity.EcoleFormation;
import inpg.sn.lms.auth.entity.Entreprise;
import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.auth.entity.Formateur;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.AlumniProfilRepository;
import inpg.sn.lms.auth.repository.EcoleFormationRepository;
import inpg.sn.lms.auth.repository.EntrepriseRepository;
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
    private final EntrepriseRepository entrepriseRepository;

    public UtilisateurCourantService(UserRepository userRepository, EtudiantRepository etudiantRepository,
            FormateurRepository formateurRepository, AlumniProfilRepository alumniProfilRepository,
            EcoleFormationRepository ecoleFormationRepository, EntrepriseRepository entrepriseRepository) {
        this.userRepository = userRepository;
        this.etudiantRepository = etudiantRepository;
        this.formateurRepository = formateurRepository;
        this.alumniProfilRepository = alumniProfilRepository;
        this.ecoleFormationRepository = ecoleFormationRepository;
        this.entrepriseRepository = entrepriseRepository;
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

    /** Profil entreprise du connecté, ou {@code null} s'il n'en a pas. */
    public Entreprise entreprise(Authentication authentication) {
        User user = utilisateur(authentication);
        return user == null ? null : entrepriseRepository.findByUserId(user.getId()).orElse(null);
    }

    /**
     * Libellé lisible d'un utilisateur : « Prénom Nom » pour une personne
     * physique (étudiant, formateur, alumni — l'alumni garde sa fiche étudiant),
     * raison sociale / nom d'école pour une personne morale, avec repli sur
     * l'identifiant. Utilisé partout où un nom d'auteur est affiché (forum,
     * documents, rendez-vous, notifications) : sans les branches école /
     * entreprise, ces comptes s'affichaient sous leur identifiant technique.
     */
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
        EcoleFormation ecole = ecoleFormationRepository.findByUserId(user.getId()).orElse(null);
        if (ecole != null) {
            return ecole.getNomEcole();
        }
        Entreprise entreprise = entrepriseRepository.findByUserId(user.getId()).orElse(null);
        if (entreprise != null) {
            return entreprise.getRaisonSociale();
        }
        // Rôles sans fiche dédiée (responsable pédagogique, évaluateur...) : à défaut de
        // vrai nom, on présente l'identifiant proprement (« test_evaluateur » -> « Test
        // Evaluateur ») plutôt que le jeton technique brut.
        return depuisIdentifiant(user.getUsername());
    }

    private static String depuisIdentifiant(String identifiant) {
        if (identifiant == null || identifiant.isBlank()) {
            return "Utilisateur";
        }
        String[] mots = identifiant.replaceAll("[._-]+", " ").trim().split("\\s+");
        StringBuilder resultat = new StringBuilder();
        for (String mot : mots) {
            if (mot.isEmpty()) {
                continue;
            }
            if (resultat.length() > 0) {
                resultat.append(' ');
            }
            resultat.append(Character.toUpperCase(mot.charAt(0))).append(mot.substring(1));
        }
        return resultat.length() == 0 ? identifiant : resultat.toString();
    }

    /** Organisation d'un compte personne morale (école ou entreprise), ou {@code null}. */
    public String organisationDe(User user) {
        if (user == null) {
            return null;
        }
        EcoleFormation ecole = ecoleFormationRepository.findByUserId(user.getId()).orElse(null);
        if (ecole != null) {
            return ecole.getNomEcole();
        }
        Entreprise entreprise = entrepriseRepository.findByUserId(user.getId()).orElse(null);
        return entreprise != null ? entreprise.getRaisonSociale() : null;
    }
}
