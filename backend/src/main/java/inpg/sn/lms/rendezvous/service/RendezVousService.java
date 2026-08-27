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
        notificationRepository.save(new Notification(destinataire, message, type, "/rendez-vous"));
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
