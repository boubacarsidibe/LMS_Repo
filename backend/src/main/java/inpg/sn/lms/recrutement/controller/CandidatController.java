package inpg.sn.lms.recrutement.controller;

import inpg.sn.lms.auth.entity.Candidat;
import inpg.sn.lms.auth.entity.Role;
import inpg.sn.lms.auth.repository.CandidatRepository;
import inpg.sn.lms.auth.repository.RoleRepository;
import inpg.sn.lms.auth.repository.UserRepository;
import inpg.sn.lms.recrutement.dto.CandidatureRequest;
import inpg.sn.lms.recrutement.dto.CandidatureResponse;
import inpg.sn.lms.recrutement.dto.PaiementRequest;
import inpg.sn.lms.recrutement.dto.PaiementResponse;
import inpg.sn.lms.recrutement.entity.BrouillonCandidature;
import inpg.sn.lms.recrutement.entity.Candidature;
import inpg.sn.lms.recrutement.entity.FichierStocke;
import inpg.sn.lms.recrutement.entity.PieceJustificative;
import inpg.sn.lms.recrutement.entity.SessionCandidature;
import inpg.sn.lms.recrutement.enums.EnumStatutCandidature;
import inpg.sn.lms.recrutement.enums.EnumTypeDocument;
import inpg.sn.lms.recrutement.repository.BrouillonCandidatureRepository;
import inpg.sn.lms.recrutement.repository.CandidatureRepository;
import inpg.sn.lms.recrutement.repository.PieceJustificativeRepository;
import inpg.sn.lms.recrutement.repository.EntretienRepository;
import inpg.sn.lms.recrutement.repository.SessionCandidatureRepository;
import inpg.sn.lms.recrutement.repository.TestAcademiqueRepository;
import inpg.sn.lms.recrutement.service.CandidatureService;
import inpg.sn.lms.recrutement.service.ConvocationPdfService;
import inpg.sn.lms.recrutement.service.EntretienService;
import inpg.sn.lms.recrutement.service.FichierStockeService;
import inpg.sn.lms.recrutement.service.LettreAdmissionPdfService;
import inpg.sn.lms.recrutement.service.NotificationCandidatService;
import inpg.sn.lms.recrutement.service.NotificationService;
import inpg.sn.lms.recrutement.service.PaiementService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/recrutement/candidat")
@CrossOrigin(origins = "http://localhost:4200")
/*
 * Autorisation rendue explicite : elle ne reposait que sur le prefixe d'URL filtre
 * dans SecurityConfig. Un endpoint ajoute hors de ce prefixe aurait ete ouvert sans
 * que rien ne le signale. Les regles d'URL restent en place et s'appliquent en plus.
 */
@PreAuthorize("isAuthenticated()")
public class CandidatController {

    private static final Logger log = LoggerFactory.getLogger(CandidatController.class);

    private final CandidatureService candidatureService;
    private final CandidatRepository candidatRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SessionCandidatureRepository sessionRepository;
    private final CandidatureRepository candidatureRepository;
    private final FichierStockeService fichierStockeService;
    private final PieceJustificativeRepository pieceJustificativeRepository;
    private final BrouillonCandidatureRepository brouillonRepository;
    private final NotificationService notificationService;
    private final NotificationCandidatService notificationCandidatService;
    private final PaiementService paiementService;
    private final TestAcademiqueRepository testAcademiqueRepository;
    private final EntretienRepository entretienRepository;
    private final EntretienService entretienService;
    private final ConvocationPdfService convocationPdfService;
    private final LettreAdmissionPdfService lettreAdmissionPdfService;

    @Value("${app.admission.email:admin@inpg.sn}")
    private String emailAdmission;

    public CandidatController(
            CandidatureService candidatureService,
            CandidatRepository candidatRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            SessionCandidatureRepository sessionRepository,
            CandidatureRepository candidatureRepository,
            FichierStockeService fichierStockeService,
            PieceJustificativeRepository pieceJustificativeRepository,
            BrouillonCandidatureRepository brouillonRepository,
            NotificationService notificationService,
            NotificationCandidatService notificationCandidatService,
            PaiementService paiementService,
            TestAcademiqueRepository testAcademiqueRepository,
            EntretienRepository entretienRepository,
            EntretienService entretienService,
            ConvocationPdfService convocationPdfService,
            LettreAdmissionPdfService lettreAdmissionPdfService) {
        this.testAcademiqueRepository = testAcademiqueRepository;
        this.entretienRepository = entretienRepository;
        this.entretienService = entretienService;
        this.convocationPdfService = convocationPdfService;
        this.lettreAdmissionPdfService = lettreAdmissionPdfService;
        this.candidatureService = candidatureService;
        this.candidatRepository = candidatRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.sessionRepository = sessionRepository;
        this.candidatureRepository = candidatureRepository;
        this.fichierStockeService = fichierStockeService;
        this.pieceJustificativeRepository = pieceJustificativeRepository;
        this.brouillonRepository = brouillonRepository;
        this.notificationService = notificationService;
        this.notificationCandidatService = notificationCandidatService;
        this.paiementService = paiementService;
    }

    /**
     * Accorde ROLE_CANDIDAT à l'utilisateur (idempotent). Un compte ne peut détenir
     * qu'un seul rôle à la fois : les rôles existants sont retirés avant l'ajout.
     */
    private void garantirRoleCandidat(inpg.sn.lms.auth.entity.User user) {
        boolean dejaCandidat = user.getRoles().stream().anyMatch(r -> "ROLE_CANDIDAT".equals(r.getCode()));
        if (dejaCandidat) {
            return;
        }
        Role roleCandidat = roleRepository.findByCode("ROLE_CANDIDAT");
        if (roleCandidat == null) {
            throw new IllegalStateException("Le rôle ROLE_CANDIDAT est introuvable.");
        }
        user.getRoles().clear();
        user.addRole(roleCandidat);
        userRepository.save(user);
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

        // Un compte Responsable Admission est strictement administratif : il ne peut pas
        // cumuler le rôle Candidat (voir AdminController#changeUserRoles pour l'exclusivité).
        boolean estAdmin = user.getRoles().stream().anyMatch(r -> "ROLE_RESPONSABLE_ADMISSION".equals(r.getCode()));
        if (estAdmin) {
            return ResponseEntity.status(403).body(Map.of(
                    "message", "Un compte Responsable Admission ne peut pas déposer de candidature."));
        }

        // 2. Chercher ou créer le Candidat avec les infos du formulaire
        final String infosPerso = request.getInformationsPersonnelles();
        Candidat candidat = candidatRepository.findByUserId(user.getId())
                .orElseGet(() -> creerCandidatDepuisUser(user, infosPerso));
        // Mettre à jour le candidat existant avec les nouvelles infos
        if (infosPerso != null && !infosPerso.isBlank()) {
            String nom = extractJsonField(infosPerso, "nom");
            String prenom = extractJsonField(infosPerso, "prenom");
            String telephone = extractJsonField(infosPerso, "telephone");
            String nationalite = extractJsonField(infosPerso, "nationalite");
            String dateNaissanceStr = extractJsonField(infosPerso, "dateNaissance");
            String niveauEtude = extractJsonField(infosPerso, "niveauEtude");
            String genre = extractJsonField(infosPerso, "genre");
            String profession = extractJsonField(infosPerso, "profession");
            String anneesExperienceStr = extractJsonField(infosPerso, "anneesExperience");
            if (!nom.isBlank()) candidat.setNom(nom);
            if (!prenom.isBlank()) candidat.setPrenom(prenom);
            if (!telephone.isBlank()) candidat.setTelephone(telephone);
            if (!nationalite.isBlank()) candidat.setNationalite(nationalite);
            if (!dateNaissanceStr.isBlank()) {
                try { candidat.setDateNaissance(LocalDate.parse(dateNaissanceStr)); } catch (Exception e) { /* ignorer */ }
            }
            if (!niveauEtude.isBlank()) candidat.setNiveauEtude(niveauEtude);
            if (!genre.isBlank()) candidat.setGenre(genre);
            if (!profession.isBlank()) candidat.setProfession(profession);
            if (!anneesExperienceStr.isBlank()) {
                try { candidat.setAnneesExperience(Integer.parseInt(anneesExperienceStr)); } catch (Exception e) { /* ignorer */ }
            }
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
            garantirRoleCandidat(user);
            brouillonRepository.findByUserId(user.getId()).ifPresent(brouillonRepository::delete);
            return ResponseEntity.ok(candidatureService.getCandidatureById(existante.getId()));
        }

        // 4. Créer une nouvelle candidature SOUMIS
        SessionCandidature session = sessionRepository.findById(request.getSessionCandidatureId())
                .orElseThrow(() -> new RuntimeException("Session non trouvée"));

        // Le TDR impose de suspendre le dépôt de candidature une fois le délai dépassé ;
        // l'administrateur garde la main pour rouvrir temporairement la session si besoin.
        if (!session.estSessionActive()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "La session « " + session.getLibelle() + " » n'est plus ouverte aux candidatures."));
        }

        Candidature candidature = new Candidature();
        candidature.setNumeroDossier(genererNumeroDossier());
        candidature.setStatut(EnumStatutCandidature.SOUMIS);
        candidature.setDateSoumission(LocalDateTime.now());
        candidature.setCandidat(candidat);
        candidature.setSessionCandidature(session);

        Candidature saved = candidatureRepository.save(candidature);
        garantirRoleCandidat(user);
        brouillonRepository.findByUserId(user.getId()).ifPresent(brouillonRepository::delete);

        // Premier filtre automatique configurable par l'admin (TDR 3.1.1) : rejette
        // immédiatement les dossiers ne remplissant pas les critères minimaux, ou fait
        // avancer directement les autres en présélection.
        String candidatEmail = user.getEmail();
        if (session.respecteCriteres(candidat)) {
            candidatureService.changerStatut(saved.getId(), EnumStatutCandidature.EN_COURS_DE_PRESELECTION, null);
            notificationCandidatService.creerNotification(candidat,
                    "Votre dossier n°" + saved.getNumeroDossier() + " a été reçu et est en cours de présélection.",
                    "STATUT_CHANGE", "/recrutement/suivi");
            if (candidatEmail != null) {
                notificationService.notifierValidationDossier(candidatEmail, saved.getNumeroDossier());
            }
        } else {
            candidatureService.changerStatut(saved.getId(), EnumStatutCandidature.REJETE,
                    "Critères de présélection automatique non remplis.");
            notificationCandidatService.creerNotification(candidat,
                    "Votre dossier n°" + saved.getNumeroDossier() + " n'a pas pu être présélectionné.",
                    "STATUT_CHANGE", "/recrutement/suivi");
            if (candidatEmail != null) {
                notificationService.notifierRejetDossier(candidatEmail, saved.getNumeroDossier(),
                        "Critères de présélection automatique non remplis.");
            }
        }

        // Notification immédiate à l'INPG (TDR 3.1.1) : ne doit pas faire échouer la
        // soumission si le serveur SMTP est momentanément indisponible.
        try {
            notificationService.envoyerEmail(emailAdmission,
                    "Nouvelle candidature reçue",
                    "Une nouvelle candidature a été soumise.\n\n"
                            + "N° de dossier : " + saved.getNumeroDossier() + "\n"
                            + "Candidat : " + candidat.getPrenom() + " " + candidat.getNom() + "\n"
                            + "Email : " + (candidatEmail != null ? candidatEmail : "N/A") + "\n"
                            + "Session : " + session.getLibelle());
        } catch (Exception e) {
            log.error("Échec de l'envoi de l'email de notification de candidature à {} : {}", emailAdmission,
                    e.getMessage(), e);
        }

        return ResponseEntity.ok(candidatureService.getCandidatureById(saved.getId()));
    }

    /**
     * Charge le brouillon du formulaire de candidature sauvegardé côté serveur
     * pour l'utilisateur connecté (reprise depuis un autre appareil).
     */
    @GetMapping("/brouillon")
    public ResponseEntity<?> getBrouillon(Authentication authentication) {
        inpg.sn.lms.auth.entity.User user = findUser(authentication);
        return brouillonRepository.findByUserId(user.getId())
                .<ResponseEntity<?>>map(b -> ResponseEntity.ok(Map.of("donneesJson", b.getDonneesJson())))
                .orElseGet(() -> ResponseEntity.ok(Map.of()));
    }

    /**
     * Sauvegarde (ou met à jour) le brouillon du formulaire de candidature côté
     * serveur. Le contenu n'est pas interprété : c'est le JSON tel qu'assemblé
     * par le frontend.
     */
    @PutMapping("/brouillon")
    public ResponseEntity<?> sauvegarderBrouillon(@RequestBody Map<String, String> body,
            Authentication authentication) {
        inpg.sn.lms.auth.entity.User user = findUser(authentication);
        String donneesJson = body.getOrDefault("donneesJson", "");
        BrouillonCandidature brouillon = brouillonRepository.findByUserId(user.getId())
                .orElseGet(() -> new BrouillonCandidature(user, donneesJson));
        brouillon.setDonneesJson(donneesJson);
        brouillon.setDateMaj(LocalDateTime.now());
        brouillonRepository.save(brouillon);
        return ResponseEntity.ok(Map.of("message", "Brouillon sauvegardé."));
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

        // Valider le fichier lui-même : le contrôle affiché côté formulaire (taille, format)
        // n'est qu'un confort UX, un client qui le contourne ne doit pas pouvoir déposer
        // n'importe quoi (exécutable, fichier surdimensionné...).
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", "Le fichier est vide."));
        }
        long tailleMaxOctets = 5L * 1024 * 1024;
        if (file.getSize() > tailleMaxOctets) {
            return ResponseEntity.badRequest()
                    .body(java.util.Map.of("message", "Le fichier dépasse la taille maximale autorisée (5 Mo)."));
        }
        String mimeType = file.getContentType();
        List<String> mimesAutorises = List.of("application/pdf", "image/jpeg", "image/png");
        if (mimeType == null || !mimesAutorises.contains(mimeType.toLowerCase())) {
            return ResponseEntity.badRequest()
                    .body(java.util.Map.of("message", "Format de fichier non autorisé (PDF, JPG ou PNG uniquement)."));
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

    /**
     * Initie le paiement des frais de dossier pour une candidature. Aucune
     * passerelle réelle (PayDunya/Touch/Kalpay) n'est connectée pour l'instant :
     * le paiement est immédiatement confirmé, comme le ferait une passerelle
     * réelle depuis le point de vue du candidat (email + notification déjà
     * câblés dans PaiementService#simulerSuccesPaiement).
     */
    @PostMapping("/paiement")
    public ResponseEntity<?> initierPaiement(@RequestBody PaiementRequest request, Authentication authentication) {
        inpg.sn.lms.auth.entity.User user = findUser(authentication);
        Candidat candidat = candidatRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Candidat non trouvé"));

        Candidature candidature = candidatureRepository.findById(request.getCandidatureId())
                .orElseThrow(() -> new RuntimeException("Candidature non trouvée"));

        if (!candidature.getCandidat().getId().equals(candidat.getId())) {
            return ResponseEntity.status(403).body(Map.of("message", "Accès non autorisé"));
        }

        PaiementResponse paiement = paiementService.initierPaiement(request);
        PaiementResponse confirme = paiementService.simulerSuccesPaiement(paiement.getId());
        return ResponseEntity.ok(confirme);
    }

    /**
     * Reçu PDF des frais de dossier. Le contrôle de propriété est délégué au service :
     * un candidat ne peut télécharger que le reçu de son propre paiement.
     */
    @GetMapping("/paiement/{id}/recu")
    public ResponseEntity<byte[]> telechargerRecu(@PathVariable Long id, Authentication authentication) {
        inpg.sn.lms.auth.entity.User user = findUser(authentication);
        Candidat candidat = candidatRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Candidat non trouvé"));

        PaiementService.Recu recu = paiementService.genererRecu(id, candidat.getId());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(recu.nomFichier(), StandardCharsets.UTF_8).build());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .headers(headers)
                .body(recu.contenu());
    }

    /**
     * Suivi par numéro de dossier.
     *
     * <p>Le numéro de dossier circule dans les courriels, sur les reçus et dans les
     * exports : ce n'est pas un secret. Sans contrôle de propriété, tout compte
     * connecté pouvait donc lire l'état civil, la date de naissance et le dossier
     * de paiement d'un autre candidat. La réponse est désormais réservée au
     * titulaire du dossier.
     */
    @GetMapping("/suivi/{numeroDossier}")
    public ResponseEntity<?> suivreAvancement(@PathVariable String numeroDossier, Authentication auth) {
        CandidatureResponse reponse = candidatureService.getCandidatureByNumeroDossier(numeroDossier);
        Optional<Candidat> candidat = candidatRepository.findByUserId(findUser(auth).getId());
        if (candidat.isEmpty() || !reponse.getCandidatId().equals(candidat.get().getId())) {
            return ResponseEntity.status(403).body(Map.of("message", "Accès non autorisé"));
        }
        return ResponseEntity.ok(reponse);
    }

    /**
     * Parcours d'évaluation du candidat : épreuves passées et entretiens planifiés.
     *
     * <p>Alimente la page de suivi, qui affichait jusqu'ici des notes, un jury et un
     * rendez-vous d'entretien écrits en dur dans le composant Angular. Les
     * appréciations du jury ne sont volontairement pas exposées : elles relèvent de
     * la délibération interne.
     */
    @GetMapping("/candidature/{id}/parcours")
    public ResponseEntity<?> parcours(@PathVariable Long id, Authentication auth) {
        CandidatureResponse candidature = candidatureService.getCandidatureById(id);
        Optional<Candidat> candidatOpt = candidatRepository.findByUserId(findUser(auth).getId());
        if (candidatOpt.isEmpty() || !candidature.getCandidatId().equals(candidatOpt.get().getId())) {
            return ResponseEntity.status(403).body(Map.of("message", "Accès non autorisé"));
        }
        Long candidatId = candidatOpt.get().getId();

        List<Map<String, Object>> tests = testAcademiqueRepository.findByCandidatId(candidatId).stream()
                .map(test -> {
                    Map<String, Object> ligne = new LinkedHashMap<>();
                    ligne.put("nomEpreuve", test.getNomEpreuve());
                    ligne.put("coefficient", test.getCoefficient());
                    ligne.put("noteObtenue", test.getNoteObtenue());
                    ligne.put("dateEpreuve", test.getDateEpreuve());
                    return ligne;
                })
                .toList();

        List<Map<String, Object>> entretiens = entretienRepository.findByCandidatId(candidatId).stream()
                .map(entretien -> {
                    Map<String, Object> ligne = new LinkedHashMap<>();
                    ligne.put("dateHeure", entretien.getDateHeure());
                    ligne.put("lieuOuLien", entretien.getLieuOuLien());
                    return ligne;
                })
                .toList();

        Map<String, Object> corps = new LinkedHashMap<>();
        corps.put("tests", tests);
        corps.put("entretiens", entretiens);
        // Seuil réellement défini sur la session, plutôt qu'une valeur d'affichage.
        corps.put("seuilReussiteTest", seuilDeLaSession(candidature));
        return ResponseEntity.ok(corps);
    }

    private Float seuilDeLaSession(CandidatureResponse candidature) {
        if (candidature.getSessionCandidatureId() == null) {
            return null;
        }
        return sessionRepository.findById(candidature.getSessionCandidatureId())
                .map(SessionCandidature::getSeuilReussiteTest)
                .orElse(null);
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
        String nationalite = "Sénégalaise";
        String dateNaissanceStr = "";
        String niveauEtude = "Non spécifié";
        String genre = "";
        String profession = "";
        String anneesExperienceStr = "";
        // Essayer d'extraire du JSON si fourni
        if (infosPerso != null && !infosPerso.isBlank()) {
            nom = extractJsonField(infosPerso, "nom");
            prenom = extractJsonField(infosPerso, "prenom");
            telephone = extractJsonField(infosPerso, "telephone");
            String jsonNationalite = extractJsonField(infosPerso, "nationalite");
            if (!jsonNationalite.isBlank()) nationalite = jsonNationalite;
            dateNaissanceStr = extractJsonField(infosPerso, "dateNaissance");
            String jsonNiveau = extractJsonField(infosPerso, "niveauEtude");
            if (!jsonNiveau.isBlank()) niveauEtude = jsonNiveau;
            genre = extractJsonField(infosPerso, "genre");
            profession = extractJsonField(infosPerso, "profession");
            anneesExperienceStr = extractJsonField(infosPerso, "anneesExperience");
        }
        Candidat c = new Candidat();
        c.setUser(user);
        c.setNom(nom.isEmpty() ? prenom : nom);
        c.setPrenom(prenom);
        c.setTelephone(telephone.isEmpty() ? "N/A" : telephone);
        c.setNationalite(nationalite);
        if (!dateNaissanceStr.isBlank()) {
            try { c.setDateNaissance(LocalDate.parse(dateNaissanceStr)); } catch (Exception e) { c.setDateNaissance(LocalDate.of(2000, 1, 1)); }
        } else {
            c.setDateNaissance(LocalDate.of(2000, 1, 1));
        }
        c.setNiveauEtude(niveauEtude);
        if (!genre.isBlank()) c.setGenre(genre);
        if (!profession.isBlank()) c.setProfession(profession);
        if (!anneesExperienceStr.isBlank()) {
            try { c.setAnneesExperience(Integer.parseInt(anneesExperienceStr)); } catch (Exception e) { /* ignorer */ }
        }
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
