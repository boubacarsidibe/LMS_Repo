package inpg.sn.lms.recrutement.controller;

import inpg.sn.lms.auth.entity.Candidat;
import inpg.sn.lms.auth.repository.CandidatRepository;
import inpg.sn.lms.auth.repository.UserRepository;
import inpg.sn.lms.recrutement.dto.CandidatureRequest;
import inpg.sn.lms.recrutement.dto.CandidatureResponse;
import inpg.sn.lms.recrutement.entity.Candidature;
import inpg.sn.lms.recrutement.entity.FichierStocke;
import inpg.sn.lms.recrutement.entity.PieceJustificative;
import inpg.sn.lms.recrutement.entity.SessionCandidature;
import inpg.sn.lms.recrutement.enums.EnumStatutCandidature;
import inpg.sn.lms.recrutement.enums.EnumTypeDocument;
import inpg.sn.lms.recrutement.repository.CandidatureRepository;
import inpg.sn.lms.recrutement.repository.PieceJustificativeRepository;
import inpg.sn.lms.recrutement.repository.SessionCandidatureRepository;
import inpg.sn.lms.recrutement.service.CandidatureService;
import inpg.sn.lms.recrutement.service.FichierStockeService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/recrutement/candidat")
@CrossOrigin(origins = "http://localhost:4200")
public class VisiteurPublicController {

    private final CandidatureService candidatureService;
    private final CandidatRepository candidatRepository;
    private final UserRepository userRepository;
    private final SessionCandidatureRepository sessionRepository;
    private final CandidatureRepository candidatureRepository;
    private final FichierStockeService fichierStockeService;
    private final PieceJustificativeRepository pieceJustificativeRepository;

    public VisiteurPublicController(
            CandidatureService candidatureService,
            CandidatRepository candidatRepository,
            UserRepository userRepository,
            SessionCandidatureRepository sessionRepository,
            CandidatureRepository candidatureRepository,
            FichierStockeService fichierStockeService,
            PieceJustificativeRepository pieceJustificativeRepository) {
        this.candidatureService = candidatureService;
        this.candidatRepository = candidatRepository;
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.candidatureRepository = candidatureRepository;
        this.fichierStockeService = fichierStockeService;
        this.pieceJustificativeRepository = pieceJustificativeRepository;
    }

    /**
     * Route unique de soumission : crée le Candidat (si besoin)
     * et la Candidature en statut SOUMIS directement.
     * Un visiteur ne peut avoir qu'une seule candidature active.
     */
    @PostMapping("/candidature/soumettre")
    public ResponseEntity<?> soumettreCandidature(
            @RequestBody CandidatureRequest request,
            Authentication authentication) {

        // 1. Récupérer l'utilisateur connecté
        inpg.sn.lms.auth.entity.User user = findUser(authentication);

        // 2. Chercher ou créer le Candidat avec les infos du formulaire
        final String infosPerso = request.getInformationsPersonnelles();
        Candidat candidat = candidatRepository.findByUserId(user.getId())
                .orElseGet(() -> creerCandidatDepuisUser(user, infosPerso));
        // Mettre à jour le candidat existant avec les nouvelles infos
        if (infosPerso != null && !infosPerso.isBlank()) {
            String nom = extractJsonField(infosPerso, "nom");
            String prenom = extractJsonField(infosPerso, "prenom");
            String telephone = extractJsonField(infosPerso, "telephone");
            if (!nom.isBlank()) candidat.setNom(nom);
            if (!prenom.isBlank()) candidat.setPrenom(prenom);
            if (!telephone.isBlank()) candidat.setTelephone(telephone);
            candidatRepository.save(candidat);
        }

        // 3. Vérifier si une candidature existe déjà (non rejetée)
        List<Candidature> existantes = candidatureRepository.findByCandidatId(candidat.getId());
        Optional<Candidature> active = existantes.stream()
                .filter(c -> c.getStatut() != EnumStatutCandidature.REJETE)
                .findFirst();

        if (active.isPresent()) {
            // Mettre à jour la candidature existante si pas encore SOUMIS
            Candidature existante = active.get();
            existante.soumettre(); // passe le statut à SOUMIS + date
            candidatureRepository.save(existante);
            return ResponseEntity.ok(candidatureService.getCandidatureById(existante.getId()));
        }

        // 4. Créer une nouvelle candidature SOUMIS
        SessionCandidature session = sessionRepository.findById(request.getSessionCandidatureId())
                .orElseThrow(() -> new RuntimeException("Session non trouvée"));

        Candidature candidature = new Candidature();
        candidature.setNumeroDossier(genererNumeroDossier());
        candidature.setStatut(EnumStatutCandidature.SOUMIS);
        candidature.setDateSoumission(LocalDateTime.now());
        candidature.setCandidat(candidat);
        candidature.setSessionCandidature(session);

        Candidature saved = candidatureRepository.save(candidature);
        return ResponseEntity.ok(candidatureService.getCandidatureById(saved.getId()));
    }

    @GetMapping("/candidatures")
    public ResponseEntity<List<CandidatureResponse>> getMesCandidatures(Authentication authentication) {
        inpg.sn.lms.auth.entity.User user = findUser(authentication);
        Optional<Candidat> candidatOpt = candidatRepository.findByUserId(user.getId());
        if (candidatOpt.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(candidatureService.getCandidaturesByCandidatId(candidatOpt.get().getId()));
    }

    @GetMapping("/candidature/{id}")
    public ResponseEntity<CandidatureResponse> getCandidature(
            @PathVariable Long id, Authentication authentication) {
        inpg.sn.lms.auth.entity.User user = findUser(authentication);
        Optional<Candidat> candidatOpt = candidatRepository.findByUserId(user.getId());
        if (candidatOpt.isEmpty()) return ResponseEntity.status(403).build();
        CandidatureResponse resp = candidatureService.getCandidatureById(id);
        if (!resp.getCandidatId().equals(candidatOpt.get().getId())) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(resp);
    }

    /**
     * Téléverser une pièce justificative pour une candidature
     */
    @PostMapping("/candidature/{candidatureId}/pieces")
    public ResponseEntity<?> televerserPiece(
            @PathVariable Long candidatureId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("typeDocument") String typeDocumentStr,
            Authentication authentication) {

        // Vérifier que le candidat est bien le propriétaire
        inpg.sn.lms.auth.entity.User user = findUser(authentication);
        Candidat candidat = candidatRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Candidat non trouvé"));

        Candidature candidature = candidatureRepository.findById(candidatureId)
                .orElseThrow(() -> new RuntimeException("Candidature non trouvée"));

        if (!candidature.getCandidat().getId().equals(candidat.getId())) {
            return ResponseEntity.status(403).body(java.util.Map.of("message", "Accès non autorisé"));
        }

        // Valider le type de document
        EnumTypeDocument typeDocument;
        try {
            typeDocument = EnumTypeDocument.valueOf(typeDocumentStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(java.util.Map.of("message", "Type de document invalide: " + typeDocumentStr));
        }

        // Enregistrer le fichier
        FichierStocke fichier = fichierStockeService.enregistrerFichier(file);

        // Créer la pièce justificative
        PieceJustificative piece = new PieceJustificative();
        piece.setTypeDocument(typeDocument);
        piece.setEstConforme(false);
        piece.setFichierStocke(fichier);
        piece.setCandidature(candidature);
        pieceJustificativeRepository.save(piece);

        return ResponseEntity.ok(java.util.Map.of(
                "message", "Pièce téléversée avec succès.",
                "fichierId", fichier.getId().toString()));
    }

    @GetMapping("/suivi/{numeroDossier}")
    public ResponseEntity<CandidatureResponse> suivreAvancement(@PathVariable String numeroDossier) {
        return ResponseEntity.ok(candidatureService.getCandidatureByNumeroDossier(numeroDossier));
    }

    // --- Méthodes privées ---

    private Candidat creerCandidatDepuisUser(inpg.sn.lms.auth.entity.User user, String infosPerso) {
        String username = user.getUsername();
        // Supprimer le suffixe numérique (ex: "ibrahima_pouye1" → "ibrahima_pouye")
        String cleanUsername = username != null ? username.replaceAll("\\d+$", "") : "";
        String prenom = cleanUsername;
        String nom = "";
        if (cleanUsername != null && cleanUsername.contains("_")) {
            String[] parts = cleanUsername.split("_", 2);
            prenom = parts[0];
            nom = parts.length > 1 ? parts[1] : "";
        }
        String telephone = "";
        // Essayer d'extraire du JSON si fourni
        if (infosPerso != null && !infosPerso.isBlank()) {
            nom = extractJsonField(infosPerso, "nom");
            prenom = extractJsonField(infosPerso, "prenom");
            telephone = extractJsonField(infosPerso, "telephone");
        }
        Candidat c = new Candidat();
        c.setUser(user);
        c.setNom(nom.isEmpty() ? prenom : nom);
        c.setPrenom(prenom);
        c.setTelephone(telephone.isEmpty() ? "N/A" : telephone);
        c.setNationalite("Sénégalaise");
        c.setDateNaissance(LocalDate.of(2000, 1, 1));
        c.setNiveauEtude("Non spécifié");
        return candidatRepository.save(c);
    }

    private inpg.sn.lms.auth.entity.User findUser(Authentication auth) {
        String name = auth.getName();
        inpg.sn.lms.auth.entity.User u = userRepository.findByUsername(name);
        if (u == null) u = userRepository.findByEmail(name);
        if (u == null) throw new RuntimeException("Utilisateur non trouvé: " + name);
        return u;
    }

    private String genererNumeroDossier() {
        return "INPG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Extrait la valeur d'un champ d'une chaîne JSON simple.
     * Ex: extractJsonField("{\"nom\":\"Pouye\"}", "nom") → "Pouye"
     */
    private String extractJsonField(String json, String fieldName) {
        String search = "\"" + fieldName + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) return "";
        start += search.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return "";
        return json.substring(start, end);
    }
}
