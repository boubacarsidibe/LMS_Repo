package inpg.sn.lms.espace_etudiant.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.EtudiantRepository;
import inpg.sn.lms.espace_etudiant.entity.Cours;
import inpg.sn.lms.espace_etudiant.entity.Notification;
import inpg.sn.lms.espace_etudiant.entity.RetourEvaluation;
import inpg.sn.lms.espace_etudiant.repository.NotificationRepository;
import inpg.sn.lms.espace_etudiant.repository.RetourEvaluationRepository;

/**
 * Rappel automatique de dépôt d'évaluation (TDR §G, besoin spécifique B4 :
 * « Notifications automatiques pour rappeler aux étudiants de remplir leurs
 * évaluations »).
 *
 * <p><strong>Première tâche planifiée de ce backend</strong> : aucune autre
 * classe n'utilisait {@code @Scheduled} avant celle-ci (vérifié par recherche
 * exhaustive) — {@code @EnableScheduling} est donc posé sur
 * {@code LmsApplication} spécifiquement pour ce besoin, précédent documenté
 * dans {@code CLAUDE.md}.
 *
 * <p>Le TDR demande un rappel « à la fin de chaque session », mais {@code Cours}
 * n'a aujourd'hui aucune notion de session/édition datée (seul le TDR §3
 * l'évoque) — inventer ce concept dépasse le périmètre de cette story
 * (décision à instruire séparément, voir {@code LOG_BLOQUANT.md}). En
 * attendant, le proxy retenu : un étudiant est relancé une fois tous les
 * {@link #COOLDOWN_JOURS} jours tant qu'il lui reste au moins un cours autorisé
 * non évalué — une seule notification agrégée par passage, pas une par cours,
 * pour ne pas noyer la cloche.
 */
@Service
public class EvaluationRappelService {

    private static final Logger log = LoggerFactory.getLogger(EvaluationRappelService.class);
    private static final long COOLDOWN_JOURS = 14;

    private final EtudiantRepository etudiantRepository;
    private final RetourEvaluationRepository retourRepository;
    private final NotificationRepository notificationRepository;
    private final HabilitationCoursService habilitation;

    public EvaluationRappelService(EtudiantRepository etudiantRepository,
            RetourEvaluationRepository retourRepository, NotificationRepository notificationRepository,
            HabilitationCoursService habilitation) {
        this.etudiantRepository = etudiantRepository;
        this.retourRepository = retourRepository;
        this.notificationRepository = notificationRepository;
        this.habilitation = habilitation;
    }

    /** Chaque jour à 7h locales : assez tôt pour être vu, sans réveiller personne. */
    @Scheduled(cron = "0 0 7 * * *")
    @Transactional
    public void rappelerEvaluationsEnAttente() {
        int notifies = 0;
        for (Etudiant etudiant : etudiantRepository.findAll()) {
            if (relancer(etudiant)) {
                notifies++;
            }
        }
        log.info("Rappel d'évaluation : {} étudiant(s) notifié(s).", notifies);
    }

    /** @return {@code true} si une notification a effectivement été envoyée. */
    private boolean relancer(Etudiant etudiant) {
        User destinataire = etudiant.getUser();
        if (destinataire == null) {
            return false;
        }
        List<Cours> autorises = habilitation.coursAutorises(etudiant);
        if (autorises.isEmpty()) {
            return false;
        }
        Set<Long> evalues = retourRepository.findByEtudiantIdOrderByDateCreationDesc(etudiant.getId()).stream()
                .map(RetourEvaluation::getCours)
                .filter(c -> c != null)
                .map(Cours::getId)
                .collect(Collectors.toSet());
        long enAttente = autorises.stream().filter(c -> !evalues.contains(c.getId())).count();
        if (enAttente == 0) {
            return false;
        }
        boolean dejaRelance = notificationRepository.existsByDestinataire_IdAndTypeAndDateCreationAfter(
                destinataire.getId(), Notification.EVALUATION_RAPPEL, LocalDateTime.now().minusDays(COOLDOWN_JOURS));
        if (dejaRelance) {
            return false;
        }
        String message = enAttente == 1
                ? "Un cours attend votre évaluation."
                : enAttente + " cours attendent votre évaluation.";
        notificationRepository.save(new Notification(
                destinataire, message, Notification.EVALUATION_RAPPEL, "/espace-etudiant/mes-evaluations"));
        return true;
    }
}
