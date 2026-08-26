package inpg.sn.lms.accreditation.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import inpg.sn.lms.accreditation.dto.AvisAccreditationDto;
import inpg.sn.lms.accreditation.dto.AvisAccreditationRequest;
import inpg.sn.lms.accreditation.dto.CritereAccreditationDto;
import inpg.sn.lms.accreditation.dto.CritereAccreditationRequest;
import inpg.sn.lms.accreditation.dto.DecisionAccreditationRequest;
import inpg.sn.lms.accreditation.dto.DemandeAccreditationEvaluateurDto;
import inpg.sn.lms.accreditation.dto.PieceAccreditationDto;
import inpg.sn.lms.accreditation.entity.AvisAccreditation;
import inpg.sn.lms.accreditation.entity.CritereAccreditation;
import inpg.sn.lms.accreditation.entity.DemandeAccreditation;
import inpg.sn.lms.accreditation.entity.PieceAccreditation;
import inpg.sn.lms.accreditation.enums.EnumAvisAccreditation;
import inpg.sn.lms.accreditation.enums.EnumStatutDemandeAccreditation;
import inpg.sn.lms.accreditation.repository.AvisAccreditationRepository;
import inpg.sn.lms.accreditation.repository.CritereAccreditationRepository;
import inpg.sn.lms.accreditation.repository.DemandeAccreditationRepository;
import inpg.sn.lms.accreditation.repository.PieceAccreditationRepository;
import inpg.sn.lms.accreditation.service.AccreditationNotificationService;
import inpg.sn.lms.accreditation.service.PieceAccreditationStorageService;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.espace_etudiant.service.UtilisateurCourantService;

/**
 * Espace évaluateur de l'accréditation des écoles de formation (TDR §F).
 *
 * <p>Vit sous {@code /api/admin/accreditation/**}, protégé par {@code hasRole
 * ('EVALUATEUR_ACCREDITATION')} à la fois au niveau de l'URL ({@code
 * SecurityConfig}, déclarée avant le joker {@code /api/admin/**}) et de la
 * méthode — double protection volontaire, même patron que {@code
 * PhotothequeController}/{@code AlumniController}.
 */
@RestController
@RequestMapping("/api/admin/accreditation")
@PreAuthorize("hasRole('EVALUATEUR_ACCREDITATION')")
public class EvaluateurAccreditationController {

    private final DemandeAccreditationRepository demandeRepository;
    private final PieceAccreditationRepository pieceRepository;
    private final CritereAccreditationRepository critereRepository;
    private final AvisAccreditationRepository avisRepository;
    private final UtilisateurCourantService utilisateurCourant;
    private final PieceAccreditationStorageService storage;
    private final AccreditationNotificationService notificationService;

    public EvaluateurAccreditationController(DemandeAccreditationRepository demandeRepository,
            PieceAccreditationRepository pieceRepository, CritereAccreditationRepository critereRepository,
            AvisAccreditationRepository avisRepository, UtilisateurCourantService utilisateurCourant,
            PieceAccreditationStorageService storage, AccreditationNotificationService notificationService) {
        this.demandeRepository = demandeRepository;
        this.pieceRepository = pieceRepository;
        this.critereRepository = critereRepository;
        this.avisRepository = avisRepository;
        this.utilisateurCourant = utilisateurCourant;
        this.storage = storage;
        this.notificationService = notificationService;
    }

    // ===================== Demandes =====================

    @GetMapping("/demandes")
    @Transactional(readOnly = true)
    public ResponseEntity<?> demandes(@RequestParam(required = false) String statut) {
        List<DemandeAccreditation> demandes;
        if (statut != null && !statut.isBlank()) {
            try {
                demandes = demandeRepository
                        .findByStatutOrderByDateSoumissionDesc(EnumStatutDemandeAccreditation.valueOf(statut));
            } catch (IllegalArgumentException invalide) {
                return ResponseEntity.badRequest().body(Map.of("message", "Statut invalide."));
            }
        } else {
            demandes = demandeRepository.findAllByOrderByDateSoumissionDesc();
        }
        return ResponseEntity.ok(demandes.stream().map(this::toDto).toList());
    }

    @GetMapping("/demandes/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> demande(@PathVariable Long id) {
        DemandeAccreditation demande = demandeRepository.findById(id).orElse(null);
        if (demande == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toDto(demande));
    }

    @GetMapping("/demandes/{id}/pieces/{pieceId}/fichier")
    @Transactional(readOnly = true)
    public ResponseEntity<org.springframework.core.io.Resource> telechargerPiece(@PathVariable Long id,
            @PathVariable Long pieceId) {
        PieceAccreditation piece = pieceRepository.findById(pieceId).orElse(null);
        if (piece == null || piece.getDemande() == null || !piece.getDemande().getId().equals(id)
                || piece.getFichierStocke() == null) {
            return ResponseEntity.notFound().build();
        }
        return EcoleAccreditationController.servirFichier(piece, storage);
    }

    /** Prend en charge une demande : passage en évaluation, l'école en est informée. */
    @PutMapping("/demandes/{id}/prendre-en-charge")
    @Transactional
    public ResponseEntity<?> prendreEnCharge(@PathVariable Long id, Authentication authentication) {
        DemandeAccreditation demande = demandeSoumise(id);
        if (demande == null) {
            return ResponseEntity.notFound().build();
        }
        User evaluateur = utilisateurCourant.utilisateur(authentication);
        demande.setStatut(EnumStatutDemandeAccreditation.EN_COURS_EVALUATION);
        demande.setEvaluateur(evaluateur);
        demandeRepository.save(demande);
        String email = demande.getEcole().getUser() != null ? demande.getEcole().getUser().getEmail() : null;
        if (email != null) {
            notificationService.notifierPriseEnCharge(email, demande.getEcole().getNomEcole());
        }
        return ResponseEntity.ok(toDto(demande));
    }

    /**
     * Dépose ou met à jour l'avis de l'évaluateur courant sur une demande en
     * cours d'évaluation (TDR §F : plusieurs évaluateurs peuvent consulter le
     * dossier et se prononcer, chacun indépendamment). Un avis par évaluateur —
     * redéposer met à jour le précédent plutôt que d'en empiler un second.
     */
    @PostMapping("/demandes/{id}/avis")
    @Transactional
    public ResponseEntity<?> deposerAvis(@PathVariable Long id, @RequestBody AvisAccreditationRequest requete,
            Authentication authentication) {
        DemandeAccreditation demande = demandeEnCours(id);
        if (demande == null) {
            return ResponseEntity.notFound().build();
        }
        EnumAvisAccreditation avis;
        try {
            avis = requete == null || requete.avis() == null
                    ? null
                    : EnumAvisAccreditation.valueOf(requete.avis());
        } catch (IllegalArgumentException invalide) {
            avis = null;
        }
        if (avis == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "L'avis doit valoir FAVORABLE ou DEFAVORABLE."));
        }
        User evaluateur = utilisateurCourant.utilisateur(authentication);
        String commentaire = requete.commentaire() == null ? null : requete.commentaire().trim();
        AvisAccreditation existant = avisRepository.findByDemandeIdAndEvaluateurId(id, evaluateur.getId())
                .orElse(null);
        if (existant != null) {
            existant.setAvis(avis);
            existant.setCommentaire(commentaire);
            existant.setDateAvis(java.time.LocalDateTime.now());
            avisRepository.save(existant);
        } else {
            avisRepository.save(new AvisAccreditation(demande, evaluateur, avis, commentaire));
        }
        return ResponseEntity.ok(toDto(demande));
    }

    @PutMapping("/demandes/{id}/accrediter")
    @Transactional
    public ResponseEntity<?> accrediter(@PathVariable Long id, @RequestBody(required = false) DecisionAccreditationRequest requete,
            Authentication authentication) {
        DemandeAccreditation demande = demandeEnCours(id);
        if (demande == null) {
            return ResponseEntity.notFound().build();
        }
        if (avisRepository.findByDemandeIdOrderByDateAvisAsc(id).isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Au moins un avis d'évaluateur est requis avant de statuer."));
        }
        trancher(demande, EnumStatutDemandeAccreditation.ACCREDITE, authentication,
                requete != null ? requete.motif() : null);
        demandeRepository.save(demande);
        String email = demande.getEcole().getUser() != null ? demande.getEcole().getUser().getEmail() : null;
        if (email != null) {
            notificationService.notifierAccreditation(email, demande.getEcole().getNomEcole());
        }
        return ResponseEntity.ok(toDto(demande));
    }

    @PutMapping("/demandes/{id}/rejeter")
    @Transactional
    public ResponseEntity<?> rejeter(@PathVariable Long id, @RequestBody DecisionAccreditationRequest requete,
            Authentication authentication) {
        if (requete == null || requete.motif() == null || requete.motif().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le motif du rejet est obligatoire."));
        }
        DemandeAccreditation demande = demandeEnCours(id);
        if (demande == null) {
            return ResponseEntity.notFound().build();
        }
        if (avisRepository.findByDemandeIdOrderByDateAvisAsc(id).isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Au moins un avis d'évaluateur est requis avant de statuer."));
        }
        trancher(demande, EnumStatutDemandeAccreditation.REJETE, authentication, requete.motif());
        demandeRepository.save(demande);
        String email = demande.getEcole().getUser() != null ? demande.getEcole().getUser().getEmail() : null;
        if (email != null) {
            notificationService.notifierRejet(email, demande.getEcole().getNomEcole(), requete.motif());
        }
        return ResponseEntity.ok(toDto(demande));
    }

    // ===================== Critères d'accréditation =====================

    @GetMapping("/criteres")
    @Transactional(readOnly = true)
    public ResponseEntity<?> criteres() {
        return ResponseEntity.ok(critereRepository.findAll().stream().map(this::toCritereDto).toList());
    }

    @PostMapping("/criteres")
    @Transactional
    public ResponseEntity<?> creerCritere(@RequestBody CritereAccreditationRequest requete) {
        if (requete.libelle() == null || requete.libelle().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le libellé du critère est obligatoire."));
        }
        CritereAccreditation critere = critereRepository.save(
                new CritereAccreditation(requete.libelle().trim(),
                        requete.description() == null ? null : requete.description().trim()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toCritereDto(critere));
    }

    @PutMapping("/criteres/{id}/desactiver")
    @Transactional
    public ResponseEntity<?> desactiverCritere(@PathVariable Long id) {
        CritereAccreditation critere = critereRepository.findById(id).orElse(null);
        if (critere == null) {
            return ResponseEntity.notFound().build();
        }
        critere.setActif(false);
        return ResponseEntity.ok(toCritereDto(critereRepository.save(critere)));
    }

    @DeleteMapping("/criteres/{id}")
    @Transactional
    public ResponseEntity<?> supprimerCritere(@PathVariable Long id) {
        if (critereRepository.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        critereRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ===================== Utilitaires =====================

    private DemandeAccreditation demandeSoumise(Long id) {
        DemandeAccreditation demande = demandeRepository.findById(id).orElse(null);
        return demande != null && demande.getStatut() == EnumStatutDemandeAccreditation.SOUMISE ? demande : null;
    }

    /** Une décision (accréditer/rejeter) porte sur une demande soumise ou déjà en évaluation. */
    private DemandeAccreditation demandeEnCours(Long id) {
        DemandeAccreditation demande = demandeRepository.findById(id).orElse(null);
        if (demande == null) {
            return null;
        }
        return demande.getStatut() == EnumStatutDemandeAccreditation.SOUMISE
                || demande.getStatut() == EnumStatutDemandeAccreditation.EN_COURS_EVALUATION
                        ? demande
                        : null;
    }

    private void trancher(DemandeAccreditation demande, EnumStatutDemandeAccreditation statut,
            Authentication authentication, String commentaire) {
        demande.setStatut(statut);
        demande.setDateDecision(java.time.LocalDateTime.now());
        demande.setEvaluateur(utilisateurCourant.utilisateur(authentication));
        if (statut == EnumStatutDemandeAccreditation.REJETE) {
            demande.setMotifRejet(commentaire);
        } else {
            demande.setCommentaireEvaluation(commentaire);
        }
    }

    private DemandeAccreditationEvaluateurDto toDto(DemandeAccreditation demande) {
        List<PieceAccreditationDto> pieces = pieceRepository.findByDemandeId(demande.getId()).stream()
                .map(piece -> new PieceAccreditationDto(piece.getId(), piece.getTypeDocument().name(),
                        piece.getFichierStocke() != null ? piece.getFichierStocke().getNomOriginal() : null))
                .toList();
        List<AvisAccreditationDto> avis = avisRepository.findByDemandeIdOrderByDateAvisAsc(demande.getId()).stream()
                .map(a -> new AvisAccreditationDto(a.getId(), a.getEvaluateur().getId(),
                        utilisateurCourant.nomAffiche(a.getEvaluateur()), a.getAvis().name(), a.getCommentaire(),
                        a.getDateAvis()))
                .toList();
        return new DemandeAccreditationEvaluateurDto(
                demande.getId(),
                demande.getStatut().name(),
                demande.getDateSoumission(),
                demande.getDateDecision(),
                demande.getMotifRejet(),
                demande.getCommentaireEvaluation(),
                demande.getEcole().getId(),
                demande.getEcole().getNomEcole(),
                pieces,
                avis);
    }

    private CritereAccreditationDto toCritereDto(CritereAccreditation critere) {
        return new CritereAccreditationDto(critere.getId(), critere.getLibelle(), critere.getDescription(),
                critere.isActif());
    }
}
