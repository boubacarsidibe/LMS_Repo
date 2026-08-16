package inpg.sn.lms.recrutement.service;

import inpg.sn.lms.exception.ResourceNotFoundException;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import inpg.sn.lms.auth.entity.Candidat;
import inpg.sn.lms.auth.repository.CandidatRepository;
import inpg.sn.lms.recrutement.entity.NotificationCandidat;
import inpg.sn.lms.recrutement.repository.NotificationCandidatRepository;

@Service
@Transactional
public class NotificationCandidatService {

    private final NotificationCandidatRepository repository;
    private final CandidatRepository candidatRepository;

    public NotificationCandidatService(NotificationCandidatRepository repository, CandidatRepository candidatRepository) {
        this.repository = repository;
        this.candidatRepository = candidatRepository;
    }

    /**
     * Crée une notification pour un candidat
     */
    public NotificationCandidat creerNotification(Candidat candidat, String message, String type, String lien) {
        NotificationCandidat notif = new NotificationCandidat(candidat, message, type, lien);
        return repository.save(notif);
    }

    /** Variante pratique pour les appelants qui n'ont que l'ID du candidat (ex: contrôleurs admin). */
    public NotificationCandidat creerNotification(Long candidatId, String message, String type, String lien) {
        Candidat candidat = candidatRepository.findById(candidatId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidat non trouvé avec l'ID: " + candidatId));
        return creerNotification(candidat, message, type, lien);
    }

    /**
     * Récupère toutes les notifications d'un candidat (triées par date décroissante)
     */
    public List<NotificationCandidat> getNotificationsByCandidatId(Long candidatId) {
        return repository.findByCandidatIdOrderByDateCreationDesc(candidatId);
    }

    /**
     * Récupère les notifications non lues d'un candidat
     */
    public List<NotificationCandidat> getNotificationsNonLues(Long candidatId) {
        return repository.findByCandidatIdAndLuFalseOrderByDateCreationDesc(candidatId);
    }

    /**
     * Compte les notifications non lues
     */
    public long countNonLues(Long candidatId) {
        return repository.countByCandidatIdAndLuFalse(candidatId);
    }

    /**
     * Marque une notification comme lue
     */
    public NotificationCandidat marquerCommeLue(Long notificationId) {
        NotificationCandidat notif = repository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification non trouvée avec l'ID: " + notificationId));
        notif.setLu(true);
        return repository.save(notif);
    }

    /**
     * Marque toutes les notifications d'un candidat comme lues
     */
    public void marquerToutesCommeLues(Long candidatId) {
        List<NotificationCandidat> nonLues = repository.findByCandidatIdAndLuFalseOrderByDateCreationDesc(candidatId);
        nonLues.forEach(n -> n.setLu(true));
        repository.saveAll(nonLues);
    }
}