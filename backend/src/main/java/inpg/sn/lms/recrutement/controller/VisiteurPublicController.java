package inpg.sn.lms.recrutement.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import inpg.sn.lms.auth.entity.Candidat;
import inpg.sn.lms.auth.repository.CandidatRepository;
import inpg.sn.lms.recrutement.dto.CandidatureRequest;
import inpg.sn.lms.recrutement.dto.CandidatureResponse;
import inpg.sn.lms.recrutement.dto.PaiementRequest;
import inpg.sn.lms.recrutement.dto.PaiementResponse;
import inpg.sn.lms.recrutement.entity.Candidature;
import inpg.sn.lms.recrutement.entity.FichierStocke;
import inpg.sn.lms.recrutement.entity.PieceJustificative;
import inpg.sn.lms.recrutement.enums.EnumTypeDocument;
import inpg.sn.lms.recrutement.repository.CandidatureRepository;
import inpg.sn.lms.recrutement.repository.PieceJustificativeRepository;
import inpg.sn.lms.recrutement.service.CandidatureService;
import inpg.sn.lms.recrutement.service.FichierStockeService;
import inpg.sn.lms.recrutement.service.PaiementService;

@RestController
@RequestMapping("/api/recrutement/candidat")
@CrossOrigin(origins = "http://localhost:4200")
public class VisiteurPublicController {

    private final CandidatureService candidatureService;
    private final FichierStockeService fichierStockeService;
    private final PaiementService paiementService;
    private final CandidatRepository candidatRepository;
    private final PieceJustificativeRepository pieceJustificativeRepository;
    private final CandidatureRepository candidatureRepository;

    public VisiteurPublicController(CandidatureService candidatureService,
            FichierStockeService fichierStockeService,
            PaiementService paiementService,
            CandidatRepository candidatRepository,
            PieceJustificativeRepository pieceJustificativeRepository,
            CandidatureRepository candidatureRepository) {
        this.candidatureService = candidatureService;
        this.fichierStockeService = fichierStockeService;
        this.paiementService = paiementService;
        this.candidatRepository = candidatRepository;
        this.pieceJustificativeRepository = pieceJustificativeRepository;
        this.candidatureRepository = candidatureRepository;
    }

    /**
     * Créer un brouillon de candidature
     */
    @PostMapping("/candidature/brouillon")
    public ResponseEntity<CandidatureResponse> creerBrouillon(@RequestBody CandidatureRequest request,
            Authentication authentication) {
        Candidat candidat = getCandidatAuthentifie(authentication);
        CandidatureResponse response = candidatureService.creerBrouillon(candidat.getId(), request);
        return ResponseEntity.ok(response);
    }

    /**
     * Soumettre une candidature
     */
    @PutMapping("/candidature/{id}/soumettre")
    public ResponseEntity<CandidatureResponse> soumettreCandidature(@PathVariable Long id) {
        CandidatureResponse response = candidatureService.soumettreCandidature(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Téléverser une pièce justificative
     */
    @PostMapping("/candidature/{candidatureId}/pieces")
    public ResponseEntity<Map<String, String>> televerserPiece(
            @PathVariable Long candidatureId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("typeDocument") String typeDocumentStr,
            Authentication authentication) {

        Candidat candidat = getCandidatAuthentifie(authentication);

        // Valider le type de document
        EnumTypeDocument typeDocument;
        try {
            typeDocument = EnumTypeDocument.valueOf(typeDocumentStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Type de document invalide: " + typeDocumentStr));
        }

        // Vérifier que la candidature appartient bien au candidat
        CandidatureResponse candidature = candidatureService.getCandidatureById(candidatureId);
        if (!candidature.getCandidatId().equals(candidat.getId())) {
            return ResponseEntity.status(403).body(Map.of("message", "Accès non autorisé à cette candidature."));
        }

        // Enregistrer le fichier
        FichierStocke fichier = fichierStockeService.enregistrerFichier(file);

        // Récupérer l'entité Candidature
        Candidature entiteCandidature = candidatureRepository.findById(candidatureId)
                .orElseThrow(() -> new RuntimeException("Candidature non trouvée avec l'ID: " + candidatureId));

        // Créer la pièce justificative liée
        PieceJustificative piece = new PieceJustificative();
        piece.setTypeDocument(typeDocument);
        piece.setEstConforme(false); // Sera validé par l'admin
        piece.setFichierStocke(fichier);
        piece.setCandidature(entiteCandidature);

        pieceJustificativeRepository.save(piece);

        return ResponseEntity.ok(Map.of("message", "Pièce téléversée avec succès.", "fichierId",
                fichier.getId().toString()));
    }

    /**
     * Initier un paiement pour une candidature
     */
    @PostMapping("/paiement")
    public ResponseEntity<PaiementResponse> payerFraisDossier(@RequestBody PaiementRequest request,
            Authentication authentication) {
        Candidat candidat = getCandidatAuthentifie(authentication);

        // Vérifier que la candidature appartient au candidat
        CandidatureResponse candidature = candidatureService.getCandidatureById(request.getCandidatureId());
        if (!candidature.getCandidatId().equals(candidat.getId())) {
            throw new RuntimeException("Accès non autorisé à cette candidature.");
        }

        PaiementResponse response = paiementService.initierPaiement(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Récupérer les candidatures du candidat connecté
     */
    @GetMapping("/candidatures")
    public ResponseEntity<List<CandidatureResponse>> getMesCandidatures(Authentication authentication) {
        Candidat candidat = getCandidatAuthentifie(authentication);
        List<CandidatureResponse> candidatures = candidatureService
                .getCandidaturesByCandidatId(candidat.getId());
        return ResponseEntity.ok(candidatures);
    }

    /**
     * Récupérer une candidature spécifique
     */
    @GetMapping("/candidature/{id}")
    public ResponseEntity<CandidatureResponse> getCandidature(@PathVariable Long id,
            Authentication authentication) {
        Candidat candidat = getCandidatAuthentifie(authentication);
        CandidatureResponse candidature = candidatureService.getCandidatureById(id);
        if (!candidature.getCandidatId().equals(candidat.getId())) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(candidature);
    }

    /**
     * Suivre l'avancement de son dossier par numéro
     */
    @GetMapping("/suivi/{numeroDossier}")
    public ResponseEntity<CandidatureResponse> suivreAvancement(@PathVariable String numeroDossier) {
        CandidatureResponse response = candidatureService.getCandidatureByNumeroDossier(numeroDossier);
        return ResponseEntity.ok(response);
    }

    private Candidat getCandidatAuthentifie(Authentication authentication) {
        String email = authentication.getName();
        return candidatRepository.findByUserEmail(email)
                .orElseThrow(() -> new RuntimeException("Candidat non trouvé pour l'utilisateur connecté."));
    }
}