package inpg.sn.lms.recrutement.service;

import inpg.sn.lms.exception.BusinessException;
import inpg.sn.lms.exception.ResourceNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import inpg.sn.lms.auth.entity.Candidat;
import inpg.sn.lms.auth.repository.CandidatRepository;
import inpg.sn.lms.recrutement.dto.CandidatureRequest;
import inpg.sn.lms.recrutement.dto.CandidatureResponse;
import inpg.sn.lms.recrutement.dto.PaiementResponse;
import inpg.sn.lms.recrutement.dto.PieceJustificativeResponse;
import inpg.sn.lms.recrutement.entity.Candidature;
import inpg.sn.lms.recrutement.entity.SessionCandidature;
import inpg.sn.lms.recrutement.enums.EnumStatutCandidature;
import inpg.sn.lms.recrutement.repository.CandidatureRepository;
import inpg.sn.lms.recrutement.repository.SessionCandidatureRepository;

@Service
@Transactional
public class CandidatureService {

    private final CandidatureRepository candidatureRepository;
    private final SessionCandidatureRepository sessionCandidatureRepository;
    private final CandidatRepository candidatRepository;

    public CandidatureService(CandidatureRepository candidatureRepository,
            SessionCandidatureRepository sessionCandidatureRepository,
            CandidatRepository candidatRepository) {
        this.candidatureRepository = candidatureRepository;
        this.sessionCandidatureRepository = sessionCandidatureRepository;
        this.candidatRepository = candidatRepository;
    }

    // creerBrouillon() et soumettreCandidature() vivaient ici sans aucun appelant :
    // CandidatController fait tout lui-même sur /candidature/soumettre. Elles étaient
    // les seules à produire le statut BROUILLON, resté de ce fait inatteignable — d'où
    // une branche de estTransitionValide() ci-dessous que rien ne peut emprunter. Le
    // brouillon réel est géré par BrouillonCandidature, une entité distincte, ce qui
    // rendait ce code d'autant plus trompeur à la lecture.

    /**
     * Met à jour le statut d'une candidature (utilisé par l'admin)
     */
    public CandidatureResponse changerStatut(Long candidatureId, EnumStatutCandidature nouveauStatut, String motif) {
        Candidature candidature = candidatureRepository.findById(candidatureId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Candidature non trouvée avec l'ID: " + candidatureId));

        // Validation des transitions autorisées
        if (!estTransitionValide(candidature.getStatut(), nouveauStatut)) {
            throw new BusinessException("Transition de statut non autorisée: "
                    + candidature.getStatut() + " → " + nouveauStatut);
        }

        candidature.setStatut(nouveauStatut);
        if (nouveauStatut == EnumStatutCandidature.REJETE && motif != null) {
            candidature.setMotifRejet(motif);
        }

        Candidature saved = candidatureRepository.save(candidature);
        return toResponse(saved);
    }

    /**
     * Récupère toutes les candidatures d'un candidat
     */
    public List<CandidatureResponse> getCandidaturesByCandidatId(Long candidatId) {
        return candidatureRepository.findByCandidatId(candidatId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Récupère toutes les candidatures d'une session
     */
    public List<CandidatureResponse> getCandidaturesBySessionId(Long sessionId) {
        return candidatureRepository.findBySessionCandidatureId(sessionId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Récupère toutes les candidatures par statut
     */
    public List<CandidatureResponse> getCandidaturesByStatut(EnumStatutCandidature statut) {
        return candidatureRepository.findByStatut(statut)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Récupère une candidature par son ID
     */
    public CandidatureResponse getCandidatureById(Long id) {
        Candidature candidature = candidatureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidature non trouvée avec l'ID: " + id));
        return toResponse(candidature);
    }

    /**
     * Récupère une candidature par numéro de dossier
     */
    public CandidatureResponse getCandidatureByNumeroDossier(String numeroDossier) {
        Candidature candidature = candidatureRepository.findByNumeroDossier(numeroDossier)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Candidature non trouvée avec le numéro: " + numeroDossier));
        return toResponse(candidature);
    }

    /**
     * Get toutes les candidatures
     */
    public List<CandidatureResponse> getAllCandidatures() {
        return candidatureRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // --- Méthodes privées ---

    private String genererNumeroDossier() {
        return "INPG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private boolean estTransitionValide(EnumStatutCandidature actuel, EnumStatutCandidature nouveau) {
        return switch (actuel) {
            case BROUILLON -> nouveau == EnumStatutCandidature.SOUMIS || nouveau == EnumStatutCandidature.BROUILLON;
            case SOUMIS -> nouveau == EnumStatutCandidature.EN_COURS_DE_PRESELECTION
                    || nouveau == EnumStatutCandidature.REJETE;
            case EN_COURS_DE_PRESELECTION ->
                nouveau == EnumStatutCandidature.ADMIS1 || nouveau == EnumStatutCandidature.REJETE;
            case ADMIS1 -> nouveau == EnumStatutCandidature.ADMIS2 || nouveau == EnumStatutCandidature.REJETE;
            case ADMIS2 -> nouveau == EnumStatutCandidature.ADMIS3 || nouveau == EnumStatutCandidature.REJETE;
            case ADMIS3, REJETE -> false; // État terminal
        };
    }

    private CandidatureResponse toResponse(Candidature c) {
        List<PieceJustificativeResponse> pieces = c.getPiecesJustificatives().stream()
                .map(p -> PieceJustificativeResponse.builder()
                        .id(p.getId())
                        .typeDocument(p.getTypeDocument())
                        .estConforme(p.isEstConforme())
                        .fichierStockeId(p.getFichierStocke() != null ? p.getFichierStocke().getId() : null)
                        .fichierNomOriginal(
                                p.getFichierStocke() != null ? p.getFichierStocke().getNomOriginal() : null)
                        .build())
                .collect(Collectors.toList());

        PaiementResponse paiementResponse = null;
        if (c.getPaiement() != null) {
            paiementResponse = PaiementResponse.builder()
                    .id(c.getPaiement().getId())
                    .referenceTransaction(c.getPaiement().getReferenceTransaction())
                    .montant(c.getPaiement().getMontant())
                    .devise(c.getPaiement().getDevise())
                    .statut(c.getPaiement().getStatut())
                    .datePaiement(c.getPaiement().getDatePaiement())
                    .methodePaiement(c.getPaiement().getMethodePaiement())
                    .build();
        }

        return CandidatureResponse.builder()
                .id(c.getId())
                .numeroDossier(c.getNumeroDossier())
                .statut(c.getStatut())
                .dateSoumission(c.getDateSoumission())
                .motifRejet(c.getMotifRejet())
                .candidatId(c.getCandidat().getId())
                .candidatNom(c.getCandidat().getNom())
                .candidatPrenom(c.getCandidat().getPrenom())
                .candidatEmail(c.getCandidat().getUser() != null ? c.getCandidat().getUser().getEmail() : null)
                .candidatGenre(c.getCandidat().getGenre())
                .candidatProfession(c.getCandidat().getProfession())
                .candidatAnneesExperience(c.getCandidat().getAnneesExperience())
                .candidatDateNaissance(c.getCandidat().getDateNaissance())
                .candidatNiveauEtude(c.getCandidat().getNiveauEtude())
                .candidatNationalite(c.getCandidat().getNationalite())
                .sessionCandidatureId(c.getSessionCandidature().getId())
                .sessionCandidatureLibelle(c.getSessionCandidature().getLibelle())
                .informationsAcademiques(c.getInformationsAcademiques())
                .informationsProfessionnelles(c.getInformationsProfessionnelles())
                .piecesJustificatives(pieces)
                .paiement(paiementResponse)
                .build();
    }
}