package inpg.sn.lms.rendezvous.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.UserRepository;
import inpg.sn.lms.espace_etudiant.entity.Notification;
import inpg.sn.lms.espace_etudiant.repository.NotificationRepository;
import inpg.sn.lms.espace_etudiant.service.UtilisateurCourantService;
import inpg.sn.lms.rendezvous.dto.CreneauDto;
import inpg.sn.lms.rendezvous.dto.PersonnelProfilDto;
import inpg.sn.lms.rendezvous.dto.RendezVousDto;
import inpg.sn.lms.rendezvous.entity.CreneauDisponibilite;
import inpg.sn.lms.rendezvous.entity.RendezVous;
import inpg.sn.lms.rendezvous.enums.EnumStatutRendezVous;
import inpg.sn.lms.rendezvous.repository.CreneauDisponibiliteRepository;
import inpg.sn.lms.rendezvous.repository.RendezVousRepository;

/**
 * Gestion des rendez-vous (TDR §J). Regroupe la logique métier partagée par
 * {@code RendezVousController} — construction des DTOs et notifications —
 * pour que le contrôleur reste une simple couche de validation/routage.
 */
@Service
public class RendezVousService {

    public static final List<String> ROLES_PERSONNEL = List.of(
            "ROLE_FORMATEUR", "ROLE_RESPONSABLE_PEDAGOGIQUE", "ROLE_RESPONSABLE_ADMISSION");

    private final UserRepository userRepository;
    private final CreneauDisponibiliteRepository creneauRepository;
    private final RendezVousRepository rendezVousRepository;
    private final NotificationRepository notificationRepository;
    private final UtilisateurCourantService utilisateurCourant;

    public RendezVousService(UserRepository userRepository, CreneauDisponibiliteRepository creneauRepository,
            RendezVousRepository rendezVousRepository, NotificationRepository notificationRepository,
            UtilisateurCourantService utilisateurCourant) {
        this.userRepository = userRepository;
        this.creneauRepository = creneauRepository;
        this.rendezVousRepository = rendezVousRepository;
        this.notificationRepository = notificationRepository;
        this.utilisateurCourant = utilisateurCourant;
    }

    public List<PersonnelProfilDto> listerPersonnel() {
        return userRepository.findDistinctByRoles_CodeInOrderByUsernameAsc(ROLES_PERSONNEL).stream()
                .map(this::toPersonnelProfilDto)
                .toList();
    }

    /**
     * Personnel restreint aux responsables pédagogiques : c'est le seul
     * interlocuteur habilité à traiter les demandes de rendez-vous des écoles
     * partenaires et des entreprises (décision client). Les écoles / entreprises
     * ne se voient donc proposer que lui.
     */
    public List<PersonnelProfilDto> listerResponsablesPedagogiques() {
        return userRepository.findDistinctByRoles_CodeInOrderByUsernameAsc(List.of("ROLE_RESPONSABLE_PEDAGOGIQUE"))
                .stream()
                .map(this::toPersonnelProfilDto)
                .toList();
    }

    /** Vrai si l'utilisateur est une école partenaire ou une entreprise (demandeur « externe »). */
    public boolean estDemandeurExterne(User user) {
        return user.getRoles().stream()
                .anyMatch(r -> "ROLE_ECOLE_FORMATION".equals(r.getCode()) || "ROLE_ENTREPRISE".equals(r.getCode()));
    }

    /** Vrai si l'utilisateur porte le rôle responsable pédagogique. */
    public boolean estResponsablePedagogique(User user) {
        return user.getRoles().stream().anyMatch(r -> "ROLE_RESPONSABLE_PEDAGOGIQUE".equals(r.getCode()));
    }

    /**
     * Vrai si l'utilisateur est un membre du personnel INPG (formateur,
     * responsable pédagogique ou responsable admission). Le personnel ne demande
     * jamais de rendez-vous : il ne fait que gérer ses créneaux et répondre aux
     * demandes reçues — c'est lui l'interlocuteur, jamais le demandeur.
     */
    public boolean estPersonnel(User user) {
        return user.getRoles().stream().anyMatch(r -> ROLES_PERSONNEL.contains(r.getCode()));
    }

    public List<CreneauDto> creneauxLibres(Long personnelId) {
        return creneauRepository
                .findByPersonnelIdAndReserveFalseAndDateHeureDebutAfterOrderByDateHeureDebutAsc(
                        personnelId, LocalDateTime.now())
                .stream().map(this::toCreneauDto).toList();
    }

    public List<CreneauDto> mesCreneaux(Long personnelId) {
        return creneauRepository.findByPersonnelIdAndDateHeureDebutAfterOrderByDateHeureDebutAsc(
                personnelId, LocalDateTime.now()).stream().map(this::toCreneauDto).toList();
    }

    public void notifier(User destinataire, String message, String type) {
        notificationRepository.save(new Notification(destinataire, message, type, lienRendezVous(destinataire)));
    }

    /**
     * URL de l'espace du destinataire pour ce rendez-vous. Chaque espace a sa
     * propre racine de route (même piège déjà rencontré pour les réponses du
     * forum et de la messagerie privée) : un lien unique {@code "/rendez-vous"}
     * ne correspond à aucune route réelle, quel que soit le rôle du destinataire.
     */
    private String lienRendezVous(User destinataire) {
        return switch (premierRoleConnu(destinataire)) {
            case "ROLE_FORMATEUR" -> "/espace-etudiant/formateur/rendez-vous";
            case "ROLE_RESPONSABLE_PEDAGOGIQUE" -> "/pedagogie/rendez-vous";
            case "ROLE_RESPONSABLE_ADMISSION" -> "/responsable-admission/rendez-vous";
            case "ROLE_ETUDIANT" -> "/espace-etudiant/rendez-vous";
            case "ROLE_ECOLE_FORMATION" -> "/accreditation/rendez-vous";
            case "ROLE_ENTREPRISE" -> "/espace-entreprise/rendez-vous";
            // Alumni et candidat n'ont aujourd'hui aucun écran de rendez-vous dédié ;
            // ce repli reste au moins correct pour un candidat.
            default -> "/recrutement/rendez-vous";
        };
    }

    public String libelleRole(User user) {
        boolean pedagogie = user.getRoles().stream().anyMatch(r -> "ROLE_RESPONSABLE_PEDAGOGIQUE".equals(r.getCode()));
        boolean admission = user.getRoles().stream().anyMatch(r -> "ROLE_RESPONSABLE_ADMISSION".equals(r.getCode()));
        boolean formateur = user.getRoles().stream().anyMatch(r -> "ROLE_FORMATEUR".equals(r.getCode()));
        if (pedagogie) {
            return "Responsable pédagogique";
        }
        if (admission) {
            return "Responsable admission";
        }
        if (formateur) {
            return "Formateur";
        }
        return "Personnel INPG";
    }

    /** Libellé lisible du profil du demandeur, pour distinguer une demande externe d'une demande interne. */
    public String libelleRoleDemandeur(User user) {
        if (user == null) {
            return "Utilisateur supprimé";
        }
        return switch (premierRoleConnu(user)) {
            case "ROLE_ECOLE_FORMATION" -> "École partenaire";
            case "ROLE_ENTREPRISE" -> "Entreprise";
            case "ROLE_RESPONSABLE_PEDAGOGIQUE" -> "Responsable pédagogique";
            case "ROLE_RESPONSABLE_ADMISSION" -> "Responsable admission";
            case "ROLE_FORMATEUR" -> "Formateur";
            case "ROLE_ETUDIANT" -> "Étudiant";
            case "ROLE_ALUMNI" -> "Alumni";
            case "ROLE_CANDIDAT" -> "Candidat";
            default -> "Utilisateur INPG";
        };
    }

    private String premierRoleConnu(User user) {
        List<String> ordre = List.of("ROLE_ECOLE_FORMATION", "ROLE_ENTREPRISE", "ROLE_RESPONSABLE_PEDAGOGIQUE",
                "ROLE_RESPONSABLE_ADMISSION", "ROLE_FORMATEUR", "ROLE_ETUDIANT", "ROLE_ALUMNI", "ROLE_CANDIDAT");
        return ordre.stream()
                .filter(code -> user.getRoles().stream().anyMatch(r -> code.equals(r.getCode())))
                .findFirst()
                .orElse("");
    }

    public CreneauDto toCreneauDto(CreneauDisponibilite creneau) {
        return new CreneauDto(creneau.getId(), creneau.getPersonnel().getId(),
                utilisateurCourant.nomAffiche(creneau.getPersonnel()), creneau.getDateHeureDebut(),
                creneau.getDateHeureFin(), creneau.isReserve());
    }

    public RendezVousDto toRendezVousDto(RendezVous rdv) {
        CreneauDisponibilite creneau = rdv.getCreneau();
        boolean afficherPropose = rdv.getStatut() == EnumStatutRendezVous.ACCEPTE
                && rdv.getDateHeureProposeeDebut() != null;
        LocalDateTime debut = afficherPropose ? rdv.getDateHeureProposeeDebut() : creneau.getDateHeureDebut();
        LocalDateTime fin = afficherPropose ? rdv.getDateHeureProposeeFin() : creneau.getDateHeureFin();
        return new RendezVousDto(
                rdv.getId(),
                creneau.getId(),
                debut,
                fin,
                creneau.getPersonnel().getId(),
                utilisateurCourant.nomAffiche(creneau.getPersonnel()),
                rdv.getDemandeur().getId(),
                utilisateurCourant.nomAffiche(rdv.getDemandeur()),
                libelleRoleDemandeur(rdv.getDemandeur()),
                utilisateurCourant.organisationDe(rdv.getDemandeur()),
                rdv.getModalite().name(),
                rdv.getLieuOuLien(),
                rdv.getMotif(),
                rdv.getStatut().name(),
                rdv.getDateHeureProposeeDebut(),
                rdv.getDateHeureProposeeFin(),
                rdv.getCommentaireReponse(),
                rdv.getDateCreation(),
                rdv.getDateReponse());
    }

    private PersonnelProfilDto toPersonnelProfilDto(User user) {
        return new PersonnelProfilDto(user.getId(), utilisateurCourant.nomAffiche(user), libelleRole(user));
    }
}
