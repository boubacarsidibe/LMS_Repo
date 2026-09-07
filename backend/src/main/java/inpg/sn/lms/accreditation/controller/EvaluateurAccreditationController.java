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
import inpg.sn.lms.accreditation.dto.EvaluationCritereDto;
import inpg.sn.lms.accreditation.dto.EvaluationCritereRequest;
import inpg.sn.lms.accreditation.dto.PieceAccreditationDto;
import inpg.sn.lms.accreditation.entity.AvisAccreditation;
import inpg.sn.lms.accreditation.entity.CritereAccreditation;
import inpg.sn.lms.accreditation.entity.DemandeAccreditation;
import inpg.sn.lms.accreditation.entity.EvaluationCritere;
import inpg.sn.lms.accreditation.entity.PieceAccreditation;
import inpg.sn.lms.accreditation.enums.EnumAppreciationCritere;
import inpg.sn.lms.accreditation.enums.EnumAvisAccreditation;
import inpg.sn.lms.accreditation.enums.EnumStatutDemandeAccreditation;
import inpg.sn.lms.accreditation.repository.AvisAccreditationRepository;
import inpg.sn.lms.accreditation.repository.CritereAccreditationRepository;
import inpg.sn.lms.accreditation.repository.DemandeAccreditationRepository;
import inpg.sn.lms.accreditation.repository.EvaluationCritereRepository;
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
    private final EvaluationCritereRepository evaluationCritereRepository;
    private final UtilisateurCourantService utilisateurCourant;
    private final PieceAccreditationStorageService storage;
    private final AccreditationNotificationService notificationService;

    public EvaluateurAccreditationController(DemandeAccreditationRepository demandeRepository,
            PieceAccreditationRepository pieceRepository, CritereAccreditationRepository critereRepository,
            AvisAccreditationRepository avisRepository, EvaluationCritereRepository evaluationCritereRepository,
            UtilisateurCourantService utilisateurCourant,
            PieceAccreditationStorageService storage, AccreditationNotificationService notificationService) {
        this.demandeRepository = demandeRepository;
        this.pieceRepository = pieceRepository;
        this.critereRepository = critereRepository;
        this.avisRepository = avisRepository;
        this.evaluationCritereRepository = evaluationCritereRepository;
        this.utilisateurCourant = utilisateurCourant;
        this.storage = storage;
        this.notificationService = notificationService;
    }

    // ===================== Demandes =====================

    @GetMapping("/demandes")
    @Transactional(readOnly = true)
    public ResponseEntity<?> demandes(@RequestParam(required = false) String statut, Authentication authentication) {
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
        Long evaluateurId = evaluateurId(authentication);
        return ResponseEntity.ok(demandes.stream().map(d -> toDto(d, evaluateurId)).toList());
    }

    @GetMapping("/demandes/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> demande(@PathVariable Long id, Authentication authentication) {
        DemandeAccreditation demande = demandeRepository.findById(id).orElse(null);
        if (demande == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toDto(demande, evaluateurId(authentication)));
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
        return ResponseEntity.ok(toDto(demande, evaluateurId(authentication)));
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
        return ResponseEntity.ok(toDto(demande, evaluateurId(authentication)));
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
        demande.setPlanActions(null);
        trancher(demande, EnumStatutDemandeAccreditation.ACCREDITE, authentication,
                requete != null ? requete.motif() : null);
        demandeRepository.save(demande);
        String email = demande.getEcole().getUser() != null ? demande.getEcole().getUser().getEmail() : null;
        if (email != null) {
            notificationService.notifierAccreditation(email, demande.getEcole().getNomEcole(),
                    demande.getDateExpiration());
        }
        return ResponseEntity.ok(toDto(demande, evaluateurId(authentication)));
    }

    /**
     * Accréditation conditionnelle (« sous réserve ») : décision favorable
     * assortie d'un plan d'actions obligatoire que l'établissement doit mettre
     * en œuvre (Cadre de référence DRCQ §IV.8). Mêmes garde-fous que
     * l'accréditation pleine (dossier en cours, au moins un avis déposé).
     */
    @PutMapping("/demandes/{id}/accrediter-sous-reserve")
    @Transactional
    public ResponseEntity<?> accrediterSousReserve(@PathVariable Long id,
            @RequestBody(required = false) DecisionAccreditationRequest requete, Authentication authentication) {
        if (requete == null || requete.planActions() == null || requete.planActions().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message",
                    "Le plan d'actions est obligatoire pour une accréditation sous réserve."));
        }
        DemandeAccreditation demande = demandeEnCours(id);
        if (demande == null) {
            return ResponseEntity.notFound().build();
        }
        if (avisRepository.findByDemandeIdOrderByDateAvisAsc(id).isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Au moins un avis d'évaluateur est requis avant de statuer."));
        }
        demande.setPlanActions(requete.planActions().trim());
        trancher(demande, EnumStatutDemandeAccreditation.ACCREDITE_SOUS_RESERVE, authentication, requete.motif());
        demandeRepository.save(demande);
        String email = demande.getEcole().getUser() != null ? demande.getEcole().getUser().getEmail() : null;
        if (email != null) {
            notificationService.notifierAccreditationSousReserve(email, demande.getEcole().getNomEcole(),
                    demande.getPlanActions(), demande.getDateExpiration());
        }
        return ResponseEntity.ok(toDto(demande, evaluateurId(authentication)));
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
        return ResponseEntity.ok(toDto(demande, evaluateurId(authentication)));
    }

    // ===================== Critères d'accréditation =====================

    @GetMapping("/criteres")
    @Transactional(readOnly = true)
    public ResponseEntity<?> criteres() {
        return ResponseEntity.ok(
                critereRepository.findAllByOrderByOrdreAscIdAsc().stream().map(this::toCritereDto).toList());
    }

    @PostMapping("/criteres")
    @Transactional
    public ResponseEntity<?> creerCritere(@RequestBody CritereAccreditationRequest requete) {
        if (requete.libelle() == null || requete.libelle().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le libellé du critère est obligatoire."));
        }
        String code = requete.code() == null || requete.code().isBlank() ? null : requete.code().trim();
        if (code != null && critereRepository.findByCode(code).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Un critère porte déjà le code « " + code + " »."));
        }
        CritereAccreditation critere = new CritereAccreditation(
                requete.libelle().trim(),
                requete.description() == null ? null : requete.description().trim());
        critere.setCode(code);
        critere.setSection(requete.section() == null || requete.section().isBlank() ? null : requete.section().trim());
        critere = critereRepository.save(critere);
        return ResponseEntity.status(HttpStatus.CREATED).body(toCritereDto(critere));
    }

    @PutMapping("/criteres/{id}/desactiver")
    @Transactional
    public ResponseEntity<?> desactiverCritere(@PathVariable Long id) {
        return basculerActif(id, false);
    }

    /** Remet un critère désactivé dans la grille des dossiers en cours. */
    @PutMapping("/criteres/{id}/reactiver")
    @Transactional
    public ResponseEntity<?> reactiverCritere(@PathVariable Long id) {
        return basculerActif(id, true);
    }

    private ResponseEntity<?> basculerActif(Long id, boolean actif) {
        CritereAccreditation critere = critereRepository.findById(id).orElse(null);
        if (critere == null) {
            return ResponseEntity.notFound().build();
        }
        critere.setActif(actif);
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

    // ===================== Grille d'évaluation (référentiel appliqué au dossier) =====================

    /**
     * Dépose ou met à jour le jugement de l'évaluateur courant sur un critère,
     * pour un dossier en cours d'évaluation. Une entrée par
     * (demande, critère, évaluateur) — redéposer met à jour (migration V30).
     */
    @PutMapping("/demandes/{id}/criteres/{critereId}")
    @Transactional
    public ResponseEntity<?> evaluerCritere(@PathVariable Long id, @PathVariable Long critereId,
            @RequestBody EvaluationCritereRequest requete, Authentication authentication) {
        DemandeAccreditation demande = demandeEnCours(id);
        if (demande == null) {
            return ResponseEntity.notFound().build();
        }
        CritereAccreditation critere = critereRepository.findById(critereId).orElse(null);
        if (critere == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Critère introuvable."));
        }
        EnumAppreciationCritere appreciation;
        try {
            appreciation = EnumAppreciationCritere.valueOf(requete == null ? "" : String.valueOf(requete.appreciation()));
        } catch (IllegalArgumentException invalide) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "L'appréciation doit valoir CONFORME, NON_CONFORME ou SANS_OBJET."));
        }
        User evaluateur = utilisateurCourant.utilisateur(authentication);
        String commentaire = requete.commentaire() == null || requete.commentaire().isBlank()
                ? null : requete.commentaire().trim();
        EvaluationCritere existant = evaluationCritereRepository
                .findByDemandeIdAndCritereIdAndEvaluateurId(id, critereId, evaluateur.getId()).orElse(null);
        if (existant != null) {
            existant.setAppreciation(appreciation);
            existant.setCommentaire(commentaire);
            existant.setDateEvaluation(java.time.LocalDateTime.now());
            evaluationCritereRepository.save(existant);
        } else {
            evaluationCritereRepository.save(
                    new EvaluationCritere(demande, critere, evaluateur, appreciation, commentaire));
        }
        return ResponseEntity.ok(toDto(demande, evaluateur.getId()));
    }

    // ===================== Utilitaires =====================

    private Long evaluateurId(Authentication authentication) {
        User user = utilisateurCourant.utilisateur(authentication);
        return user == null ? null : user.getId();
    }

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

    /** Durée de validité d'une accréditation de programme (Cadre de référence DRCQ §V.4). */
    private static final int VALIDITE_ACCREDITATION_ANNEES = 3;

    private void trancher(DemandeAccreditation demande, EnumStatutDemandeAccreditation statut,
            Authentication authentication, String commentaire) {
        java.time.LocalDateTime maintenant = java.time.LocalDateTime.now();
        demande.setStatut(statut);
        demande.setDateDecision(maintenant);
        demande.setEvaluateur(utilisateurCourant.utilisateur(authentication));
        if (statut == EnumStatutDemandeAccreditation.REJETE) {
            demande.setMotifRejet(commentaire);
            demande.setDateExpiration(null);
        } else {
            demande.setCommentaireEvaluation(commentaire);
            // Accréditation pleine ou conditionnelle : valable 3 ans à compter de la décision.
            demande.setDateExpiration(maintenant.toLocalDate().plusYears(VALIDITE_ACCREDITATION_ANNEES));
        }
    }

    private DemandeAccreditationEvaluateurDto toDto(DemandeAccreditation demande, Long evaluateurId) {
        List<PieceAccreditationDto> pieces = pieceRepository.findByDemandeId(demande.getId()).stream()
                .map(piece -> new PieceAccreditationDto(piece.getId(), piece.getTypeDocument().name(),
                        piece.getFichierStocke() != null ? piece.getFichierStocke().getNomOriginal() : null))
                .toList();
        List<AvisAccreditationDto> avis = avisRepository.findByDemandeIdOrderByDateAvisAsc(demande.getId()).stream()
                .map(a -> new AvisAccreditationDto(a.getId(), a.getEvaluateur().getId(),
                        utilisateurCourant.nomAffiche(a.getEvaluateur()), a.getAvis().name(), a.getCommentaire(),
                        a.getDateAvis()))
                .toList();
        List<CritereAccreditation> actifs = critereRepository.findByActifTrueOrderByOrdreAscIdAsc();
        List<CritereAccreditationDto> criteresActifs = actifs == null ? List.of()
                : actifs.stream().map(this::toCritereDto).toList();
        List<EvaluationCritere> grille = evaluateurId == null ? null
                : evaluationCritereRepository.findByDemandeIdAndEvaluateurIdOrderByCritereIdAsc(demande.getId(),
                        evaluateurId);
        List<EvaluationCritereDto> maGrille = grille == null ? List.of()
                : grille.stream()
                        .map(e -> new EvaluationCritereDto(e.getCritere().getId(), e.getCritere().getLibelle(),
                                e.getCritere().getDescription(), e.getAppreciation().name(), e.getCommentaire(),
                                e.getDateEvaluation()))
                        .toList();
        var ecole = demande.getEcole();
        return new DemandeAccreditationEvaluateurDto(
                demande.getId(),
                demande.getStatut().name(),
                demande.getDateSoumission(),
                demande.getDateDecision(),
                demande.getDateExpiration(),
                demande.getMotifRejet(),
                demande.getCommentaireEvaluation(),
                demande.getPlanActions(),
                ecole.getId(),
                ecole.getNomEcole(),
                ecole.getAdresse(),
                ecole.getTelephone(),
                ecole.getSiteWeb(),
                ecole.getUser() != null ? ecole.getUser().getEmail() : null,
                demande.getIntituleProgramme(),
                demande.getNiveauProgramme() != null ? demande.getNiveauProgramme().name() : null,
                demande.getTypeStructure() != null ? demande.getTypeStructure().name() : null,
                demande.getEffectifCohorte(),
                demande.getDateDebutCohorte(),
                pieces,
                avis,
                criteresActifs,
                maGrille);
    }

    private CritereAccreditationDto toCritereDto(CritereAccreditation critere) {
        return new CritereAccreditationDto(critere.getId(), critere.getCode(), critere.getSection(),
                critere.getLibelle(), critere.getDescription(), critere.getOrdre(), critere.isActif());
    }
}
