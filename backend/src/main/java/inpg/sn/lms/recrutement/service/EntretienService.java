package inpg.sn.lms.recrutement.service;

import org.springframework.security.access.AccessDeniedException;
import inpg.sn.lms.exception.BusinessException;
import inpg.sn.lms.exception.ResourceNotFoundException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import inpg.sn.lms.auth.entity.Candidat;
import inpg.sn.lms.auth.repository.CandidatRepository;
import inpg.sn.lms.recrutement.dto.EntretienRequest;
import inpg.sn.lms.recrutement.dto.EntretienResponse;
import inpg.sn.lms.recrutement.entity.Entretien;
import inpg.sn.lms.recrutement.repository.EntretienRepository;

@Service
@Transactional
public class EntretienService {

    private static final Logger log = LoggerFactory.getLogger(EntretienService.class);
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final EntretienRepository entretienRepository;
    private final CandidatRepository candidatRepository;
    private final NotificationService notificationService;
    private final NotificationCandidatService notificationCandidatService;

    public EntretienService(EntretienRepository entretienRepository, CandidatRepository candidatRepository,
            NotificationService notificationService, NotificationCandidatService notificationCandidatService) {
        this.entretienRepository = entretienRepository;
        this.candidatRepository = candidatRepository;
        this.notificationService = notificationService;
        this.notificationCandidatService = notificationCandidatService;
    }

    public EntretienResponse creerEntretien(EntretienRequest request) {
        Candidat candidat = candidatRepository.findById(request.getCandidatId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidat non trouvé avec l'ID: " + request.getCandidatId()));

        Entretien entretien = new Entretien();
        entretien.setDateHeure(request.getDateHeure());
        entretien.setLieuOuLien(request.getLieuOuLien());
        entretien.setNoteEntretien(request.getNoteEntretien());
        entretien.setAppreciationGlobale(request.getAppreciationGlobale());
        entretien.setCandidat(candidat);

        Entretien saved = entretienRepository.save(entretien);

        // Convocation automatique du candidat dès la planification de l'entretien (TDR 3.1.3).
        if (saved.getDateHeure() != null) {
            String dateFormatee = saved.getDateHeure().format(DATETIME_FORMAT);
            String emailCandidat = candidat.getUser() != null ? candidat.getUser().getEmail() : null;
            if (emailCandidat != null) {
                try {
                    notificationService.notifierConvocationEntretien(emailCandidat, dateFormatee, saved.getLieuOuLien());
                } catch (Exception e) {
                    log.error("Échec de l'envoi de la convocation à l'entretien à {} : {}", emailCandidat,
                            e.getMessage(), e);
                }
            }
            notificationCandidatService.creerNotification(candidat,
                    "Vous êtes convoqué(e) à l'entretien le " + dateFormatee + " (" + saved.getLieuOuLien() + ").",
                    "ENTRETIEN_CONVOCATION", "/recrutement/suivi");
        }

        return toResponse(saved);
    }

    public EntretienResponse modifierEntretien(Long id, EntretienRequest request) {
        Entretien entretien = entretienRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entretien non trouvé avec l'ID: " + id));

        entretien.setDateHeure(request.getDateHeure());
        entretien.setLieuOuLien(request.getLieuOuLien());
        entretien.setNoteEntretien(request.getNoteEntretien());
        entretien.setAppreciationGlobale(request.getAppreciationGlobale());

        Entretien saved = entretienRepository.save(entretien);
        return toResponse(saved);
    }

    /**
     * Saisie de la seule note, sans toucher au reste de l'entretien.
     *
     * <p>Après une convocation groupée, les notes se saisissent en série dans la liste :
     * passer par {@link #modifierEntretien} obligerait à renvoyer date, lieu et appréciation
     * à chaque frappe, et les écraserait avec ce que le formulaire connaît — c'est-à-dire
     * rien, dans une grille où seule la note est éditée.
     */
    public EntretienResponse modifierNote(Long id, Float note) {
        Entretien entretien = entretienRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entretien non trouvé avec l'ID: " + id));
        if (note != null && (note < 0 || note > 20)) {
            throw new BusinessException("La note d'entretien doit être comprise entre 0 et 20.");
        }
        entretien.setNoteEntretien(note);
        return toResponse(entretienRepository.save(entretien));
    }

    public List<EntretienResponse> getEntretiensByCandidatId(Long candidatId) {
        return entretienRepository.findByCandidatId(candidatId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<EntretienResponse> getAllEntretiens() {
        return entretienRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public void supprimerEntretien(Long id) {
        if (!entretienRepository.existsById(id)) {
            throw new ResourceNotFoundException("Entretien non trouvé avec l'ID: " + id);
        }
        entretienRepository.deleteById(id);
    }

    public EntretienResponse confirmerPresence(Long entretienId, Long candidatId) {
        Entretien entretien = entretienRepository.findById(entretienId)
                .orElseThrow(() -> new ResourceNotFoundException("Entretien non trouvé avec l'ID: " + entretienId));
        if (!entretien.getCandidat().getId().equals(candidatId)) {
            throw new AccessDeniedException("Accès non autorisé à cet entretien.");
        }
        entretien.setConfirmeParCandidat(true);
        Entretien saved = entretienRepository.save(entretien);
        return toResponse(saved);
    }

    public EntretienResponse demanderReport(Long entretienId, Long candidatId, String motif) {
        Entretien entretien = entretienRepository.findById(entretienId)
                .orElseThrow(() -> new ResourceNotFoundException("Entretien non trouvé avec l'ID: " + entretienId));
        if (!entretien.getCandidat().getId().equals(candidatId)) {
            throw new AccessDeniedException("Accès non autorisé à cet entretien.");
        }
        entretien.setMotifReport(motif != null ? motif : "Demande de report sans motif spécifié");
        entretien.setConfirmeParCandidat(false);
        Entretien saved = entretienRepository.save(entretien);
        return toResponse(saved);
    }

    public Entretien getEntretienEntity(Long id) {
        return entretienRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entretien non trouvé avec l'ID: " + id));
    }

    private EntretienResponse toResponse(Entretien e) {
        return EntretienResponse.builder()
                .id(e.getId())
                .dateHeure(e.getDateHeure())
                .lieuOuLien(e.getLieuOuLien())
                .noteEntretien(e.getNoteEntretien())
                .appreciationGlobale(e.getAppreciationGlobale())
                .confirmeParCandidat(e.isConfirmeParCandidat())
                .motifReport(e.getMotifReport())
                .candidatId(e.getCandidat().getId())
                .build();
    }
}