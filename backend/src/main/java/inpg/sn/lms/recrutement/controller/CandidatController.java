package inpg.sn.lms.recrutement.controller;

import inpg.sn.lms.exception.ResourceNotFoundException;
import inpg.sn.lms.auth.entity.Candidat;
import inpg.sn.lms.auth.entity.Role;
import inpg.sn.lms.auth.repository.CandidatRepository;
import inpg.sn.lms.auth.repository.RoleRepository;
import inpg.sn.lms.auth.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import inpg.sn.lms.exception.BusinessException;
import inpg.sn.lms.recrutement.dto.CandidatureRequest;
import inpg.sn.lms.recrutement.dto.CandidatureResponse;
import inpg.sn.lms.recrutement.dto.InfosPersonnellesDto;
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
import org.springframework.http.HttpStatus;
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
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/recrutement/candidat")
/*
 * Autorisation rendue explicite : elle ne reposait que sur le prefixe d'URL filtre
 * dans SecurityConfig. Un endpoint ajoute hors de ce prefixe aurait ete ouvert sans
 * que rien ne le signale. Les regles d'URL restent en place et s'appliquent en plus.
 */
@PreAuthorize("isAuthenticated()")
public class CandidatController {

    private static final Logger log = LoggerFactory.getLogger(CandidatController.class);

    /**
     * Rôles qui ouvrent déjà un espace de travail dans l'application. Un compte qui
     * en détient un ne peut pas déposer de candidature : {@link #garantirRoleCandidat}
     * vide les rôles avant d'accorder CANDIDAT, ce qui retirerait à ce compte l'accès
     * à ses cours, ses notes et ses certificats — sans qu'il puisse le rétablir seul.
     *
     * <p>Le cas normal reste intact : un compte fraîchement créé n'a aucun rôle
     * (voir UserService#createUser) et passe donc librement.
     */
    private static final Set<String> ROLES_OUVRANT_UN_ESPACE = Set.of(
            "ROLE_ETUDIANT",
            "ROLE_FORMATEUR",
            "ROLE_RESPONSABLE_PEDAGOGIQUE",
            "ROLE_RESPONSABLE_ADMISSION");

    /**
     * États depuis lesquels une nouvelle soumission reste légitime, c'est-à-dire tant
     * que l'administration ne s'est pas prononcée. Le TDR demande explicitement qu'un
     * candidat puisse « revenir plus tard pour poursuivre et finaliser sa candidature » ;
     * il ne demande pas qu'il puisse revenir sur une décision déjà rendue.
     */
    private static final Set<EnumStatutCandidature> STATUTS_ENCORE_MODIFIABLES = Set.of(
            EnumStatutCandidature.BROUILLON,
            EnumStatutCandidature.SOUMIS);

    /** Repli quand le formulaire ne fournit pas de date : la colonne est NOT NULL en base. */
    private static final LocalDate DATE_NAISSANCE_PAR_DEFAUT = LocalDate.of(2000, 1, 1);

    /**
     * Plafond par pièce justificative, plus strict que la limite multipart globale
     * (10 Mo, {@code spring.servlet.multipart.max-file-size}) : un scan de diplôme ou
     * de pièce d'identité tient largement dans 5 Mo.
     */
    private static final long TAILLE_MAX_PIECE_OCTETS = 5L * 1024 * 1024;

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
    private final ObjectMapper objectMapper;

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
            LettreAdmissionPdfService lettreAdmissionPdfService,
            ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
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

        // Le contrôle ne visait que le Responsable Admission, alors que garantirRoleCandidat()
        // plus bas vide les rôles de N'IMPORTE QUEL compte. Un étudiant qui postulait à un
        // second programme perdait donc silencieusement l'accès à son espace, sans recours.
        // L'exclusivité des rôles reste la règle (voir AdminController#changeUserRoles) :
        // c'est le périmètre du garde-fou qui était trop étroit.
        Optional<Role> roleDejaAttribue = user.getRoles().stream()
                .filter(r -> ROLES_OUVRANT_UN_ESPACE.contains(r.getCode()))
                .findFirst();
        if (roleDejaAttribue.isPresent()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "message", "Ce compte donne déjà accès à l'espace « " + roleDejaAttribue.get().getLibelle()
                            + " ». Pour déposer une candidature, utilisez un compte distinct."));
        }

        // 2. Un dossier déjà examiné se verrouille.
        //    Ce contrôle précède délibérément toute écriture : placé plus bas, il aurait
        //    laissé le bloc suivant réécrire l'état civil du candidat (nom, date de
        //    naissance, niveau d'étude...) avant de refuser la soumission — soit une
        //    modification de dossier obtenue malgré le refus.
        Optional<Candidat> candidatExistant = candidatRepository.findByUserId(user.getId());
        Optional<Candidature> dossierVerrouille = candidatExistant
                .map(c -> candidatureRepository.findByCandidatId(c.getId()))
                .orElseGet(List::of)
                .stream()
                .filter(c -> c.getStatut() != EnumStatutCandidature.REJETE)
                .filter(c -> !STATUTS_ENCORE_MODIFIABLES.contains(c.getStatut()))
                .findFirst();
        if (dossierVerrouille.isPresent()) {
            Candidature verrouille = dossierVerrouille.get();
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "message", "Votre dossier n°" + verrouille.getNumeroDossier()
                            + " est déjà en cours d'examen : il ne peut plus être soumis à nouveau.",
                    "statut", verrouille.getStatut().name()));
        }

        // 3. Chercher ou créer le Candidat avec les infos du formulaire
        InfosPersonnellesDto infos = lireInfosPersonnelles(request.getInformationsPersonnelles());
        Candidat candidat;
        if (candidatExistant.isPresent()) {
            candidat = candidatExistant.get();
            appliquerInfosPersonnelles(candidat, infos);
            candidatRepository.save(candidat);
        } else {
            // creerCandidatDepuisUser applique déjà ces informations et enregistre.
            candidat = creerCandidatDepuisUser(user, infos);
        }

        // 4. Vérifier si une candidature existe déjà (non rejetée)
        List<Candidature> existantes = candidatureRepository.findByCandidatId(candidat.getId());
        Optional<Candidature> active = existantes.stream()
                .filter(c -> c.getStatut() != EnumStatutCandidature.REJETE)
                .findFirst();

        if (active.isPresent()) {
            Candidature existante = active.get();
            // soumettre() réécrit le statut à SOUMIS sans regarder d'où l'on vient : appelé
            // sur un dossier déjà présélectionné, il le ramenait dans la pile « à traiter »
            // et effaçait la décision du responsable d'admission. Le seul garde-fou est
            // celui de l'étape 2, qui a déjà écarté ces dossiers — ne pas appeler cette
            // méthode depuis un autre chemin sans reproduire ce contrôle.
            existante.soumettre(); // passe le statut à SOUMIS + date
            candidatureRepository.save(existante);
            garantirRoleCandidat(user);
            brouillonRepository.findByUserId(user.getId()).ifPresent(brouillonRepository::delete);
            return ResponseEntity.ok(candidatureService.getCandidatureById(existante.getId()));
        }

        // 5. Créer une nouvelle candidature SOUMIS
        SessionCandidature session = sessionRepository.findById(request.getSessionCandidatureId())
                .orElseThrow(() -> new ResourceNotFoundException("Session non trouvée"));

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
                .orElseThrow(() -> new ResourceNotFoundException("Candidat non trouvé"));

        Candidature candidature = candidatureRepository.findById(candidatureId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidature non trouvée"));

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

        // Seule la taille est contrôlée ici : c'est la seule règle que
        // FichierStockeService ne porte pas. Le fichier vide et le format étaient
        // vérifiés en double, avec une liste plus étroite (sans WEBP) et fondée sur le
        // seul type MIME — que le client déclare et falsifie trivialement.
        // ValidationFichier, appelée juste en dessous, vérifie d'abord l'extension,
        // qui est ce qui décide réellement du Content-Type de réponse.
        if (file.getSize() > TAILLE_MAX_PIECE_OCTETS) {
            return ResponseEntity.badRequest()
                    .body(java.util.Map.of("message", "Le fichier dépasse la taille maximale autorisée (5 Mo)."));
        }

        // Enregistrer le fichier (extension, type MIME et chemin de destination validés)
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
     * câblés dans PaiementService#confirmerPaiement).
     */
    @PostMapping("/paiement")
    public ResponseEntity<?> initierPaiement(@RequestBody PaiementRequest request, Authentication authentication) {
        inpg.sn.lms.auth.entity.User user = findUser(authentication);
        Candidat candidat = candidatRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidat non trouvé"));

        Candidature candidature = candidatureRepository.findById(request.getCandidatureId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidature non trouvée"));

        if (!candidature.getCandidat().getId().equals(candidat.getId())) {
            return ResponseEntity.status(403).body(Map.of("message", "Accès non autorisé"));
        }

        PaiementResponse paiement = paiementService.initierPaiement(request);
        // Deux etapes distinctes, meme si la simulation les enchaine : c'est la couture ou
        // se branchera l'agregateur reel, qui confirmera de facon asynchrone par webhook.
        PaiementResponse confirme = paiementService.confirmerPaiement(paiement.getId());
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
                .orElseThrow(() -> new ResourceNotFoundException("Candidat non trouvé"));

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

    private Candidat creerCandidatDepuisUser(inpg.sn.lms.auth.entity.User user, InfosPersonnellesDto infos) {
        String username = user.getUsername();
        // Supprimer le suffixe numérique (ex: "ibrahima_pouye1" → "ibrahima_pouye")
        String cleanUsername = username != null ? username.replaceAll("\\d+$", "") : "";
        String prenom = cleanUsername;
        String nom = "";
        if (cleanUsername.contains("_")) {
            String[] parts = cleanUsername.split("_", 2);
            prenom = parts[0];
            nom = parts.length > 1 ? parts[1] : "";
        }

        Candidat c = new Candidat();
        c.setUser(user);
        // Valeurs de repli, remplacées ensuite par ce que le formulaire a réellement fourni.
        c.setNom(nom.isEmpty() ? prenom : nom);
        c.setPrenom(prenom);
        c.setTelephone("N/A");
        c.setNationalite("Sénégalaise");
        c.setDateNaissance(DATE_NAISSANCE_PAR_DEFAUT);
        c.setNiveauEtude("Non spécifié");

        appliquerInfosPersonnelles(c, infos);
        return candidatRepository.save(c);
    }

    /**
     * Reporte sur le candidat les champs réellement renseignés du formulaire. Un champ
     * absent laisse la valeur existante en place — c'est ce qui permet de compléter un
     * dossier en plusieurs fois sans effacer ce qui avait déjà été saisi.
     */
    private void appliquerInfosPersonnelles(Candidat candidat, InfosPersonnellesDto infos) {
        if (infos == null) {
            return;
        }
        if (infos.aNom()) candidat.setNom(infos.nom());
        if (infos.aPrenom()) candidat.setPrenom(infos.prenom());
        if (infos.aTelephone()) candidat.setTelephone(infos.telephone());
        if (infos.aNationalite()) candidat.setNationalite(infos.nationalite());
        if (infos.dateNaissance() != null) candidat.setDateNaissance(infos.dateNaissance());
        if (infos.aNiveauEtude()) candidat.setNiveauEtude(infos.niveauEtude());
        if (infos.aGenre()) candidat.setGenre(infos.genre());
        if (infos.aProfession()) candidat.setProfession(infos.profession());
        if (infos.anneesExperience() != null) candidat.setAnneesExperience(infos.anneesExperience());
    }

    /**
     * Désérialise l'état civil transmis par le formulaire.
     *
     * <p>Le découpage manuel de la chaîne qui tenait ce rôle ne reconnaissait que les
     * valeurs entre guillemets : un {@code "anneesExperience": 5} était silencieusement
     * perdu, alors que ce champ sert au filtre automatique de présélection. Un JSON
     * illisible est désormais signalé au candidat plutôt qu'ignoré.
     */
    private InfosPersonnellesDto lireInfosPersonnelles(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, InfosPersonnellesDto.class);
        } catch (JsonProcessingException e) {
            log.warn("Informations personnelles illisibles : {}", e.getOriginalMessage());
            throw new BusinessException(
                    "Les informations personnelles transmises n'ont pas pu être lues. Vérifiez le formulaire.");
        }
    }

    private inpg.sn.lms.auth.entity.User findUser(Authentication auth) {
        String name = auth.getName();
        inpg.sn.lms.auth.entity.User u = userRepository.findByUsername(name);
        if (u == null) u = userRepository.findByEmail(name);
        if (u == null) throw new ResourceNotFoundException("Utilisateur non trouvé: " + name);
        return u;
    }

    private String genererNumeroDossier() {
        return "INPG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
