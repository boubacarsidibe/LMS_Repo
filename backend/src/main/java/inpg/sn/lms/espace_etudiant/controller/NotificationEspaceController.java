package inpg.sn.lms.espace_etudiant.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.espace_etudiant.entity.Notification;
import inpg.sn.lms.espace_etudiant.repository.NotificationRepository;
import inpg.sn.lms.espace_etudiant.service.UtilisateurCourantService;

/**
 * Cloche de notifications, commune à l'étudiant, au formateur, au responsable
 * pédagogique et au responsable admission.
 *
 * <p>Une cloche existait, mais liée au <strong>candidat</strong> et servie par
 * {@code /api/recrutement/candidat/notifications} : inutilisable ailleurs. Celle-ci
 * est adossée à l'utilisateur, donc valable dans les trois espaces.
 *
 * <p>Chaque endpoint ne lit que les notifications du <strong>connecté</strong> : aucun
 * identifiant de destinataire n'est accepté en paramètre, faute de quoi il suffirait
 * d'en changer pour lire le courrier d'autrui.
 */
@RestController
@RequestMapping("/api/espace-etudiant/notifications")
@PreAuthorize("hasAnyRole('ETUDIANT','FORMATEUR','RESPONSABLE_PEDAGOGIQUE','RESPONSABLE_ADMISSION')")
public class NotificationEspaceController {

    private final NotificationRepository notificationRepository;
    private final UtilisateurCourantService utilisateurCourant;

    public NotificationEspaceController(NotificationRepository notificationRepository,
            UtilisateurCourantService utilisateurCourant) {
        this.notificationRepository = notificationRepository;
        this.utilisateurCourant = utilisateurCourant;
    }

    /** Les cinquante dernières : au-delà, une cloche n'est plus consultée mais fouillée. */
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<?> mesNotifications(Authentication authentication) {
        User user = utilisateurCourant.utilisateur(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(
                notificationRepository.findTop50ByDestinataire_IdOrderByDateCreationDesc(user.getId())
                        .stream().map(NotificationEspaceController::toDto).toList());
    }

    /** Compteur seul : appelé à chaque changement d'écran, il doit rester léger. */
    @GetMapping("/count-non-lues")
    @Transactional(readOnly = true)
    public ResponseEntity<?> compteur(Authentication authentication) {
        User user = utilisateurCourant.utilisateur(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(Map.of("nonLues",
                notificationRepository.countByDestinataire_IdAndLuFalse(user.getId())));
    }

    @PostMapping("/{id}/lu")
    @Transactional
    public ResponseEntity<?> marquerLu(@PathVariable Long id, Authentication authentication) {
        User user = utilisateurCourant.utilisateur(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Notification notification = notificationRepository.findById(id).orElse(null);
        // Introuvable et « pas la vôtre » renvoient la même réponse : distinguer les deux
        // permettrait de deviner l'existence des notifications d'autrui.
        if (notification == null || notification.getDestinataire() == null
                || !user.getId().equals(notification.getDestinataire().getId())) {
            return ResponseEntity.notFound().build();
        }
        notification.setLu(true);
        return ResponseEntity.ok(toDto(notificationRepository.save(notification)));
    }

    @PostMapping("/tout-lu")
    @Transactional
    public ResponseEntity<?> toutLu(Authentication authentication) {
        User user = utilisateurCourant.utilisateur(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(Map.of("marquees", notificationRepository.marquerToutLu(user.getId())));
    }

    private static Map<String, Object> toDto(Notification notification) {
        return Map.of(
                "id", notification.getId(),
                "message", notification.getMessage(),
                "type", notification.getType(),
                "lu", notification.isLu(),
                "dateCreation", notification.getDateCreation() == null
                        ? LocalDateTime.now().toString()
                        : notification.getDateCreation().toString(),
                "lien", notification.getLien() == null ? "" : notification.getLien());
    }

    /** Sert au test structurel, qui vérifie que tout endpoint ouvre sa transaction. */
    static List<String> routes() {
        return List.of("", "/count-non-lues", "/{id}/lu", "/tout-lu");
    }
}
