package inpg.sn.lms.recrutement.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

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

    private final PaiementRepository paiementRepository;
    private final CandidatureRepository candidatureRepository;

    public PaiementService(PaiementRepository paiementRepository,
            CandidatureRepository candidatureRepository) {
        this.paiementRepository = paiementRepository;
        this.candidatureRepository = candidatureRepository;
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