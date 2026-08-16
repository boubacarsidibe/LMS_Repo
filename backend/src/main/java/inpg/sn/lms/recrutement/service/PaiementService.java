package inpg.sn.lms.recrutement.service;

import inpg.sn.lms.exception.DuplicateResourceException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import inpg.sn.lms.exception.BusinessException;
import inpg.sn.lms.exception.ResourceNotFoundException;
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
    private final RecuPaiementService recuPaiementService;

    @Value("${app.admission.email:admin@inpg.sn}")
    private String emailAdmission;

    /** Frais de dossier, en unités de la devise ci-dessous. Fixés par l'établissement. */
    @Value("${app.recrutement.frais-dossier:25000}")
    private String fraisDossier;

    @Value("${app.recrutement.devise:XOF}")
    private String deviseFraisDossier;

    public PaiementService(PaiementRepository paiementRepository,
            CandidatureRepository candidatureRepository,
            NotificationService notificationService,
            NotificationCandidatService notificationCandidatService,
            RecuPaiementService recuPaiementService) {
        this.paiementRepository = paiementRepository;
        this.candidatureRepository = candidatureRepository;
        this.notificationService = notificationService;
        this.notificationCandidatService = notificationCandidatService;
        this.recuPaiementService = recuPaiementService;
    }

    /**
     * Initie un paiement pour une candidature (simulation)
     */
    public PaiementResponse initierPaiement(PaiementRequest request) {
        Candidature candidature = candidatureRepository.findById(request.getCandidatureId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Candidature non trouvée avec l'ID: " + request.getCandidatureId()));

        // Vérifier qu'il n'y a pas déjà un paiement en cours
        paiementRepository.findByCandidatureId(request.getCandidatureId())
                .ifPresent(p -> {
                    throw new DuplicateResourceException("Un paiement existe déjà pour cette candidature.");
                });

        Paiement paiement = new Paiement();
        paiement.setReferenceTransaction("PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        // Tarif et devise proviennent de la configuration, jamais de la requête.
        paiement.setMontant(new BigDecimal(fraisDossier));
        paiement.setDevise(deviseFraisDossier);
        paiement.setStatut(EnumStatutPaiement.EN_ATTENTE);
        paiement.setMethodePaiement(
                request.getMethodePaiement() != null && !request.getMethodePaiement().isBlank()
                        ? request.getMethodePaiement()
                        : "Mobile Money");
        paiement.setCandidature(candidature);

        Paiement saved = paiementRepository.save(paiement);
        return toResponse(saved);
    }

    /**
     * Simule le succès du paiement
     */
    public PaiementResponse simulerSuccesPaiement(Long paiementId) {
        Paiement paiement = paiementRepository.findById(paiementId)
                .orElseThrow(() -> new ResourceNotFoundException("Paiement non trouvé avec l'ID: " + paiementId));

        if (!paiement.effectuerPaiement()) {
            throw new BusinessException("Le paiement ne peut pas être finalisé (statut actuel: " + paiement.getStatut()
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
                        () -> new BusinessException("Aucun paiement trouvé pour la candidature: " + candidatureId));
        return toResponse(paiement);
    }

    /** Historique complet des paiements (admin), avec le contexte du dossier associé. */
    public List<PaiementResponse> getAllPaiements() {
        return paiementRepository.findAll()
                .stream()
                .map(this::toResponseAvecContexte)
                .collect(Collectors.toList());
    }

    /** Reçu prêt à être servi : le contenu du PDF et le nom de fichier proposé au téléchargement. */
    public record Recu(byte[] contenu, String nomFichier) {
    }

    /**
     * Génère le reçu PDF d'un paiement, après vérification que le candidat
     * authentifié en est bien le propriétaire.
     *
     * @param candidatId candidat authentifié ; {@code null} pour un accès back-office,
     *                   qui n'est pas soumis au contrôle de propriété.
     */
    @Transactional(readOnly = true)
    public Recu genererRecu(Long paiementId, Long candidatId) {
        Paiement paiement = paiementRepository.findById(paiementId)
                .orElseThrow(() -> new ResourceNotFoundException("Paiement", "id", paiementId));

        Candidature candidature = paiement.getCandidature();

        if (candidatId != null) {
            boolean proprietaire = candidature != null
                    && candidature.getCandidat() != null
                    && candidatId.equals(candidature.getCandidat().getId());
            if (!proprietaire) {
                throw new AccessDeniedException("Ce paiement n'appartient pas au candidat authentifié.");
            }
        }

        if (paiement.getStatut() != EnumStatutPaiement.SUCCES) {
            throw new BusinessException(
                    "Aucun reçu disponible : le paiement n'est pas validé (statut actuel : " + paiement.getStatut()
                            + ").");
        }

        String numeroDossier = candidature != null ? candidature.getNumeroDossier() : null;
        String nomFichier = numeroDossier != null && !numeroDossier.isBlank()
                ? "recu-" + numeroDossier + ".pdf"
                : "recu-paiement.pdf";

        return new Recu(recuPaiementService.genererRecu(paiement), nomFichier);
    }

    private PaiementResponse toResponse(Paiement p) {
        return PaiementResponse.builder()
                .id(p.getId())
                .referenceTransaction(p.getReferenceTransaction())
                .montant(p.getMontant())
                .devise(p.getDevise())
                .statut(p.getStatut())
                .datePaiement(p.getDatePaiement())
                .methodePaiement(p.getMethodePaiement())
                .build();
    }

    private PaiementResponse toResponseAvecContexte(Paiement p) {
        PaiementResponse base = toResponse(p);
        Candidature candidature = p.getCandidature();
        if (candidature != null) {
            base.setCandidatureId(candidature.getId());
            base.setNumeroDossier(candidature.getNumeroDossier());
            if (candidature.getCandidat() != null) {
                base.setCandidatNom(candidature.getCandidat().getNom());
                base.setCandidatPrenom(candidature.getCandidat().getPrenom());
            }
        }
        return base;
    }
}