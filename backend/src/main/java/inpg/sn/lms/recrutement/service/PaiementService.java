package inpg.sn.lms.recrutement.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import inpg.sn.lms.recrutement.dto.PaiementRequest;
import inpg.sn.lms.recrutement.dto.PaiementResponse;
import inpg.sn.lms.recrutement.entity.Candidature;
import inpg.sn.lms.recrutement.entity.Paiement;
import inpg.sn.lms.recrutement.enums.EnumStatutPaiement;
import inpg.sn.lms.recrutement.repository.CandidatureRepository;
import inpg.sn.lms.recrutement.repository.PaiementRepository;

@Service
@Transactional
public class PaiementService {

    private static final Logger log = LoggerFactory.getLogger(PaiementService.class);

    private final PaiementRepository paiementRepository;
    private final CandidatureRepository candidatureRepository;
    private final NotificationService notificationService;
    private final NotificationCandidatService notificationCandidatService;

    @Value("${app.admission.email:admin@inpg.sn}")
    private String emailAdmission;

    public PaiementService(PaiementRepository paiementRepository,
            CandidatureRepository candidatureRepository,
            NotificationService notificationService,
            NotificationCandidatService notificationCandidatService) {
        this.paiementRepository = paiementRepository;
        this.candidatureRepository = candidatureRepository;
        this.notificationService = notificationService;
        this.notificationCandidatService = notificationCandidatService;
    }

    /**
     * Initie un paiement pour une candidature (simulation)
     */
    public PaiementResponse initierPaiement(PaiementRequest request) {
        Candidature candidature = candidatureRepository.findById(request.getCandidatureId())
                .orElseThrow(() -> new RuntimeException(
                        "Candidature non trouvée avec l'ID: " + request.getCandidatureId()));

        // Vérifier qu'il n'y a pas déjà un paiement en cours
        paiementRepository.findByCandidatureId(request.getCandidatureId())
                .ifPresent(p -> {
                    throw new RuntimeException("Un paiement existe déjà pour cette candidature.");
                });

        Paiement paiement = new Paiement();
        paiement.setReferenceTransaction("PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        paiement.setMontant(request.getMontant() != null ? request.getMontant() : new BigDecimal("25000"));
        paiement.setDevise(request.getDevise() != null ? request.getDevise() : "XOF");
        paiement.setStatut(EnumStatutPaiement.EN_ATTENTE);
        paiement.setCandidature(candidature);

        Paiement saved = paiementRepository.save(paiement);
        return toResponse(saved);
    }

    /**
     * Simule le succès du paiement
     */
    public PaiementResponse simulerSuccesPaiement(Long paiementId) {
        Paiement paiement = paiementRepository.findById(paiementId)
                .orElseThrow(() -> new RuntimeException("Paiement non trouvé avec l'ID: " + paiementId));

        if (!paiement.effectuerPaiement()) {
            throw new RuntimeException("Le paiement ne peut pas être finalisé (statut actuel: " + paiement.getStatut()
                    + ").");
        }

        Paiement saved = paiementRepository.save(paiement);

        Candidature candidature = saved.getCandidature();
        if (candidature != null && candidature.getCandidat() != null) {
            String numeroDossier = candidature.getNumeroDossier();
            String emailCandidat = candidature.getCandidat().getUser() != null
                    ? candidature.getCandidat().getUser().getEmail()
                    : null;
            if (emailCandidat != null) {
                notificationService.notifierPaiementSucces(emailCandidat, numeroDossier);
            }
            notificationCandidatService.creerNotification(candidature.getCandidat().getId(),
                    "Le paiement des frais de dossier pour la candidature n°" + numeroDossier + " est confirmé.",
                    "PAIEMENT", "/recrutement/suivi");
            try {
                notificationService.envoyerEmail(emailAdmission,
                        "Paiement confirmé",
                        "Le paiement du dossier n°" + numeroDossier + " (" + saved.getMontant() + " "
                                + saved.getDevise() + ") a été confirmé.");
            } catch (Exception e) {
                log.error("Échec de l'envoi de l'email de confirmation de paiement à {} : {}", emailAdmission,
                        e.getMessage(), e);
            }
        }

        return toResponse(saved);
    }

    /**
     * Récupère le paiement d'une candidature
     */
    public PaiementResponse getPaiementByCandidatureId(Long candidatureId) {
        Paiement paiement = paiementRepository.findByCandidatureId(candidatureId)
                .orElseThrow(
                        () -> new RuntimeException("Aucun paiement trouvé pour la candidature: " + candidatureId));
        return toResponse(paiement);
    }

    private PaiementResponse toResponse(Paiement p) {
        return PaiementResponse.builder()
                .id(p.getId())
                .referenceTransaction(p.getReferenceTransaction())
                .montant(p.getMontant())
                .devise(p.getDevise())
                .statut(p.getStatut())
                .datePaiement(p.getDatePaiement())
                .build();
    }
}