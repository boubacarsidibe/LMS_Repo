package inpg.sn.lms.recrutement.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import inpg.sn.lms.auth.entity.Candidat;
import inpg.sn.lms.auth.repository.CandidatRepository;
import inpg.sn.lms.auth.repository.UserRepository;
import inpg.sn.lms.recrutement.entity.NotificationCandidat;
import inpg.sn.lms.recrutement.service.NotificationCandidatService;

@RestController
@RequestMapping("/api/recrutement/candidat")
/*
 * Autorisation rendue explicite : elle ne reposait que sur le prefixe d'URL filtre
 * dans SecurityConfig. Un endpoint ajoute hors de ce prefixe aurait ete ouvert sans
 * que rien ne le signale. Les regles d'URL restent en place et s'appliquent en plus.
 */
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationCandidatService notificationCandidatService;
    private final CandidatRepository candidatRepository;
    private final UserRepository userRepository;

    public NotificationController(NotificationCandidatService notificationCandidatService,
            CandidatRepository candidatRepository,
            UserRepository userRepository) {
        this.notificationCandidatService = notificationCandidatService;
        this.candidatRepository = candidatRepository;
        this.userRepository = userRepository;
    }

    /**
     * Récupère toutes les notifications du candidat connecté
     */
    @GetMapping("/notifications")
    public ResponseEntity<List<NotificationCandidat>> getNotifications(Authentication authentication) {
        Candidat candidat = findCandidat(authentication);
        return ResponseEntity.ok(notificationCandidatService.getNotificationsByCandidatId(candidat.getId()));
    }

    /**
     * Compte les notifications non lues
     */
    @GetMapping("/notifications/count")
    public ResponseEntity<Map<String, Long>> countNonLues(Authentication authentication) {
        Candidat candidat = findCandidat(authentication);
        long count = notificationCandidatService.countNonLues(candidat.getId());
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Marque une notification comme lue
     */
    @PostMapping("/notifications/{id}/lu")
    public ResponseEntity<NotificationCandidat> marquerLue(@PathVariable Long id, Authentication authentication) {
        Candidat candidat = findCandidat(authentication);
        NotificationCandidat notif = notificationCandidatService.marquerCommeLue(id);
        // Vérifie que la notification appartient bien au candidat
        if (!notif.getCandidat().getId().equals(candidat.getId())) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(notif);
    }

    /**
     * Marque toutes les notifications comme lues
     */
    @PostMapping("/notifications/tout-lu")
    public ResponseEntity<Map<String, String>> marquerToutLu(Authentication authentication) {
        Candidat candidat = findCandidat(authentication);
        notificationCandidatService.marquerToutesCommeLues(candidat.getId());
        return ResponseEntity.ok(Map.of("message", "Toutes les notifications marquées comme lues"));
    }

    private Candidat findCandidat(Authentication auth) {
        String name = auth.getName();
        inpg.sn.lms.auth.entity.User u = userRepository.findByUsername(name);
        if (u == null)
            u = userRepository.findByEmail(name);
        if (u == null)
            throw new RuntimeException("Utilisateur non trouvé: " + name);
        return candidatRepository.findByUserId(u.getId())
                .orElseThrow(() -> new RuntimeException("Candidat non trouvé pour l'utilisateur: " + name));
    }
}