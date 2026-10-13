package inpg.sn.lms.recrutement.controller;

import inpg.sn.lms.exception.BusinessException;
import inpg.sn.lms.exception.ResourceNotFoundException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;

import inpg.sn.lms.auth.entity.Candidat;
import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.auth.entity.Role;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.CandidatRepository;
import inpg.sn.lms.auth.repository.EtudiantRepository;
import inpg.sn.lms.auth.repository.RoleRepository;
import inpg.sn.lms.auth.repository.UserRepository;
import inpg.sn.lms.recrutement.dto.CandidatureResponse;
import inpg.sn.lms.recrutement.dto.ConvocationEntretienGroupeeRequest;
import inpg.sn.lms.recrutement.dto.ConvocationTestGroupeeRequest;
import inpg.sn.lms.recrutement.dto.CritereAdmissionRequest;
import inpg.sn.lms.recrutement.dto.CritereAdmissionResponse;
import inpg.sn.lms.recrutement.dto.EntretienRequest;
import inpg.sn.lms.recrutement.dto.EntretienResponse;
import inpg.sn.lms.recrutement.dto.EtapeDossierResponse;
import inpg.sn.lms.recrutement.dto.FiltreCriteresResponse;
import inpg.sn.lms.recrutement.dto.PaiementResponse;
import inpg.sn.lms.recrutement.dto.PieceJustificativeResponse;
import inpg.sn.lms.recrutement.dto.ModifierConformitePieceRequest;
import inpg.sn.lms.recrutement.dto.RapportRecrutementDto;
import inpg.sn.lms.recrutement.dto.SessionCandidatureRequest;
import inpg.sn.lms.recrutement.dto.SessionCandidatureResponse;
import inpg.sn.lms.recrutement.dto.TestAcademiqueRequest;
import inpg.sn.lms.recrutement.dto.TestAcademiqueResponse;
import inpg.sn.lms.recrutement.entity.Candidature;
import inpg.sn.lms.recrutement.entity.FichierStocke;
import inpg.sn.lms.recrutement.entity.NotificationTemplate;
import inpg.sn.lms.recrutement.entity.PieceJustificative;
import inpg.sn.lms.recrutement.dto.DocumentAttenduDto;
import inpg.sn.lms.recrutement.enums.EnumEtapeQms;
import inpg.sn.lms.recrutement.enums.EnumStatutCandidature;
import inpg.sn.lms.recrutement.enums.EnumStatutPaiement;
import inpg.sn.lms.recrutement.enums.EnumStatutVerificationPiece;
import inpg.sn.lms.recrutement.enums.EnumTypeDocument;
import inpg.sn.lms.recrutement.enums.EnumTypeNotification;
import inpg.sn.lms.recrutement.repository.PieceJustificativeRepository;
import inpg.sn.lms.recrutement.service.CandidatureService;
import inpg.sn.lms.recrutement.service.JournalEtapeService;
import inpg.sn.lms.recrutement.service.EntretienService;
import inpg.sn.lms.recrutement.service.ExcelExportService;
import inpg.sn.lms.recrutement.service.FichierStockeService;
import inpg.sn.lms.recrutement.service.NotificationCandidatService;
import inpg.sn.lms.recrutement.service.NotificationService;
import inpg.sn.lms.recrutement.service.NotificationTemplateService;
import inpg.sn.lms.recrutement.service.PaiementService;
import inpg.sn.lms.recrutement.service.RapportRecrutementPdfService;
import inpg.sn.lms.recrutement.service.SessionCandidatureService;
import inpg.sn.lms.recrutement.service.TestAcademiqueService;

@RestController
@RequestMapping("/api/admin/recrutement")
/*
 * Autorisation rendue explicite : elle ne reposait que sur le prefixe d'URL filtre
 * dans SecurityConfig. Un endpoint ajoute hors de ce prefixe aurait ete ouvert sans
 * que rien ne le signale. Les regles d'URL restent en place et s'appliquent en plus.
 */
@PreAuthorize("hasRole('RESPONSABLE_ADMISSION')")
public class AdminRecrutementController {

    private static final Logger log = LoggerFactory.getLogger(AdminRecrutementController.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final SessionCandidatureService sessionService;
    private final CandidatureService candidatureService;
    private final PaiementService paiementService;
    private final TestAcademiqueService testAcademiqueService;
    private final EntretienService entretienService;
    private final NotificationService notificationService;
    private final NotificationCandidatService notificationCandidatService;
    private final ExcelExportService excelExportService;
    private final PieceJustificativeRepository pieceJustificativeRepository;
    private final FichierStockeService fichierStockeService;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final EtudiantRepository etudiantRepository;
    private final CandidatRepository candidatRepository;
    private final NotificationTemplateService notificationTemplateService;
    private final JournalEtapeService journalEtapeService;
    private final RapportRecrutementPdfService rapportRecrutementPdfService;
    private final PasswordEncoder passwordEncoder;

    public AdminRecrutementController(SessionCandidatureService sessionService,
            CandidatureService candidatureService,
            PaiementService paiementService,
            TestAcademiqueService testAcademiqueService,
            EntretienService entretienService,
            NotificationService notificationService,
            NotificationCandidatService notificationCandidatService,
            ExcelExportService excelExportService,
            PieceJustificativeRepository pieceJustificativeRepository,
            FichierStockeService fichierStockeService,
            RoleRepository roleRepository,
            UserRepository userRepository,
            EtudiantRepository etudiantRepository,
            CandidatRepository candidatRepository,
            NotificationTemplateService notificationTemplateService,
            JournalEtapeService journalEtapeService,
            RapportRecrutementPdfService rapportRecrutementPdfService,
            PasswordEncoder passwordEncoder) {
        this.journalEtapeService = journalEtapeService;
        this.sessionService = sessionService;
        this.candidatureService = candidatureService;
        this.paiementService = paiementService;
        this.testAcademiqueService = testAcademiqueService;
        this.entretienService = entretienService;
        this.notificationService = notificationService;
        this.notificationCandidatService = notificationCandidatService;
        this.excelExportService = excelExportService;
        this.pieceJustificativeRepository = pieceJustificativeRepository;
        this.fichierStockeService = fichierStockeService;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.etudiantRepository = etudiantRepository;
        this.candidatRepository = candidatRepository;
        this.notificationTemplateService = notificationTemplateService;
        this.rapportRecrutementPdfService = rapportRecrutementPdfService;
        this.passwordEncoder = passwordEncoder;
    }

    private ResponseEntity<byte[]> telechargerXlsx(byte[] contenu, String nomFichier) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(nomFichier, StandardCharsets.UTF_8).build());
        return ResponseEntity.ok().contentType(XLSX).headers(headers).body(contenu);
    }

    private ResponseEntity<byte[]> telechargerPdf(byte[] contenu, String nomFichier) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(nomFichier, StandardCharsets.UTF_8).build());
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).headers(headers).body(contenu);
    }

    private String nvl(String valeur) {
        return valeur == null ? "" : valeur;
    }

    // ===================== SESSIONS =====================

    @GetMapping("/sessions")
    public ResponseEntity<List<SessionCandidatureResponse>> getAllSessions() {
        return ResponseEntity.ok(sessionService.getAllSessions());
    }

    @GetMapping("/sessions/{id}")
    public ResponseEntity<SessionCandidatureResponse> getSession(@PathVariable Long id) {
        return ResponseEntity.ok(sessionService.getSessionById(id));
    }

    @PostMapping("/sessions")
    public ResponseEntity<SessionCandidatureResponse> creerSession(
            @RequestBody SessionCandidatureRequest request) {
        return ResponseEntity.ok(sessionService.creerSession(request));
    }

    @PutMapping("/sessions/{id}")
    public ResponseEntity<SessionCandidatureResponse> modifierSession(@PathVariable Long id,
            @RequestBody SessionCandidatureRequest request) {
        return ResponseEntity.ok(sessionService.modifierSession(id, request));
    }

    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<Map<String, String>> supprimerSession(@PathVariable Long id) {
        sessionService.supprimerSession(id);
        return ResponseEntity.ok(Map.of("message", "Session supprimée avec succès."));
    }

    /**
     * Déclenche le filtre automatique de premier niveau (TDR 3.1.1) sur les dossiers
     * SOUMIS de la session : évalue les critères de base et personnalisés, bascule en
     * présélection ou rejette. Action manuelle du responsable admission — jamais
     * déclenchée à la soumission elle-même (voir CandidatController#soumettreCandidature).
     */
    @PostMapping("/sessions/{id}/appliquer-filtre-criteres")
    public ResponseEntity<FiltreCriteresResponse> appliquerFiltreCriteres(@PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(candidatureService.appliquerFiltreCriteres(id, agentCourant(authentication)));
    }

    // ===================== CRITÈRES PERSONNALISÉS =====================

    @GetMapping("/sessions/{id}/criteres")
    public ResponseEntity<List<CritereAdmissionResponse>> getCriteresDeSession(@PathVariable Long id) {
        return ResponseEntity.ok(sessionService.getCriteresDeSession(id));
    }

    @PostMapping("/sessions/{id}/criteres")
    public ResponseEntity<CritereAdmissionResponse> creerCritere(@PathVariable Long id,
            @RequestBody CritereAdmissionRequest request) {
        return ResponseEntity.ok(sessionService.creerCritere(id, request));
    }

    @GetMapping("/criteres/{critereId}")
    public ResponseEntity<CritereAdmissionResponse> getCritere(@PathVariable Long critereId) {
        return ResponseEntity.ok(sessionService.getCritere(critereId));
    }

    @PutMapping("/criteres/{critereId}")
    public ResponseEntity<CritereAdmissionResponse> modifierCritere(@PathVariable Long critereId,
            @RequestBody CritereAdmissionRequest request) {
        return ResponseEntity.ok(sessionService.modifierCritere(critereId, request));
    }

    @DeleteMapping("/criteres/{critereId}")
    public ResponseEntity<CritereAdmissionResponse> retirerCritere(@PathVariable Long critereId) {
        return ResponseEntity.ok(sessionService.retirerCritere(critereId));
    }

    @PutMapping("/criteres/{critereId}/reactiver")
    public ResponseEntity<CritereAdmissionResponse> reactiverCritere(@PathVariable Long critereId) {
        return ResponseEntity.ok(sessionService.reactiverCritere(critereId));
    }

    /** Colonnes disponibles pour l'export "Sessions", dans l'ordre proposé au front. */
    private static final Map<String, String> COLONNES_SESSIONS = new java.util.LinkedHashMap<>();
    static {
        COLONNES_SESSIONS.put("libelle", "Libellé");
        COLONNES_SESSIONS.put("dateOuverture", "Date d'ouverture");
        COLONNES_SESSIONS.put("dateFermeture", "Date de fermeture");
        COLONNES_SESSIONS.put("estOuverte", "Ouverte");
        COLONNES_SESSIONS.put("estActive", "Active");
        COLONNES_SESSIONS.put("nbCandidatures", "Nb candidatures");
    }

    @GetMapping("/sessions/export")
    public ResponseEntity<byte[]> exporterSessions(
            @RequestParam(required = false) List<String> colonnes) {
        List<String> cles = (colonnes == null || colonnes.isEmpty())
                ? new ArrayList<>(COLONNES_SESSIONS.keySet())
                : colonnes.stream().filter(COLONNES_SESSIONS::containsKey).toList();
        if (cles.isEmpty()) cles = new ArrayList<>(COLONNES_SESSIONS.keySet());

        List<SessionCandidatureResponse> sessions = sessionService.getAllSessions();
        List<CandidatureResponse> toutesCandidatures = candidatureService.getAllCandidatures();

        List<String> entetes = cles.stream().map(COLONNES_SESSIONS::get).toList();
        List<List<Object>> lignes = new ArrayList<>();
        for (SessionCandidatureResponse s : sessions) {
            List<Object> ligne = new ArrayList<>();
            for (String cle : cles) {
                ligne.add(valeurColonneSession(s, cle, toutesCandidatures));
            }
            lignes.add(ligne);
        }

        List<String> categories = sessions.stream().map(SessionCandidatureResponse::getLibelle).toList();
        List<Double> valeurs = sessions.stream()
                .map(s -> (double) toutesCandidatures.stream()
                        .filter(c -> c.getSessionCandidatureId().equals(s.getId())).count())
                .toList();

        byte[] fichier = excelExportService.genererClasseur("Sessions", entetes, lignes,
                "Candidatures par session", categories, valeurs, "Nb candidatures");
        return telechargerXlsx(fichier, "sessions_recrutement.xlsx");
    }

    private Object valeurColonneSession(SessionCandidatureResponse s, String cle,
            List<CandidatureResponse> toutesCandidatures) {
        return switch (cle) {
            case "libelle" -> nvl(s.getLibelle());
            case "dateOuverture" -> s.getDateOuverture() != null ? s.getDateOuverture().format(DATE_FORMAT) : "";
            case "dateFermeture" -> s.getDateFermeture() != null ? s.getDateFermeture().format(DATE_FORMAT) : "";
            case "estOuverte" -> s.isEstOuverte() ? "Oui" : "Non";
            case "estActive" -> s.isEstActive() ? "Oui" : "Non";
            case "nbCandidatures" -> (int) toutesCandidatures.stream()
                    .filter(c -> c.getSessionCandidatureId().equals(s.getId())).count();
            default -> "";
        };
    }

    // ===================== CANDIDATURES =====================

    @GetMapping("/candidatures")
    public ResponseEntity<List<CandidatureResponse>> getAllCandidatures() {
        return ResponseEntity.ok(candidatureService.getAllCandidatures());
    }

    @GetMapping("/candidatures/page")
    public ResponseEntity<Page<CandidatureResponse>> getCandidaturesPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int taille) {
        return ResponseEntity.ok(candidatureService.getCandidaturesPage(page, taille));
    }

    @GetMapping("/candidatures/{id}")
    public ResponseEntity<CandidatureResponse> getCandidature(@PathVariable Long id) {
        return ResponseEntity.ok(candidatureService.getCandidatureById(id));
    }

    @GetMapping("/candidatures/statut/{statut}")
    public ResponseEntity<List<CandidatureResponse>> getCandidaturesByStatut(
            @PathVariable String statut) {
        EnumStatutCandidature enumStatut = EnumStatutCandidature.valueOf(statut.toUpperCase());
        return ResponseEntity.ok(candidatureService.getCandidaturesByStatut(enumStatut));
    }

    /** Colonnes disponibles pour l'export "Candidatures", dans l'ordre proposé au front. */
    private static final Map<String, String> COLONNES_CANDIDATURES = new java.util.LinkedHashMap<>();
    static {
        COLONNES_CANDIDATURES.put("numeroDossier", "N° Dossier");
        COLONNES_CANDIDATURES.put("nom", "Nom");
        COLONNES_CANDIDATURES.put("prenom", "Prénom");
        COLONNES_CANDIDATURES.put("email", "Email");
        COLONNES_CANDIDATURES.put("session", "Session");
        COLONNES_CANDIDATURES.put("statut", "Statut");
        COLONNES_CANDIDATURES.put("dateSoumission", "Date de soumission");
        COLONNES_CANDIDATURES.put("motifRejet", "Motif de rejet");
        COLONNES_CANDIDATURES.put("genre", "Genre");
        COLONNES_CANDIDATURES.put("niveauEtude", "Niveau d'études");
        COLONNES_CANDIDATURES.put("profession", "Profession");
        COLONNES_CANDIDATURES.put("anneesExperience", "Années d'expérience");
        COLONNES_CANDIDATURES.put("doc_CV", "CV");
        COLONNES_CANDIDATURES.put("doc_DIPLOME", "Diplôme");
        COLONNES_CANDIDATURES.put("doc_RELEVE_NOTES", "Relevé de notes");
        COLONNES_CANDIDATURES.put("doc_PIECE_IDENTITE", "Pièce d'identité");
        COLONNES_CANDIDATURES.put("doc_LETTRE_MOTIVATION", "Lettre de motivation");
    }

    @GetMapping("/candidatures/export")
    public ResponseEntity<byte[]> exporterCandidatures(
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) List<String> colonnes) {
        List<CandidatureResponse> candidatures = listerParStatuts(statut);

        List<String> cles = (colonnes == null || colonnes.isEmpty())
                ? List.of("numeroDossier", "nom", "prenom", "email", "session", "statut", "dateSoumission", "motifRejet")
                : colonnes.stream().filter(COLONNES_CANDIDATURES::containsKey).toList();
        if (cles.isEmpty()) cles = new ArrayList<>(COLONNES_CANDIDATURES.keySet());

        List<String> entetes = cles.stream().map(COLONNES_CANDIDATURES::get).toList();
        List<List<Object>> lignes = new ArrayList<>();
        for (CandidatureResponse c : candidatures) {
            List<Object> ligne = new ArrayList<>();
            for (String cle : cles) {
                ligne.add(valeurColonneCandidature(c, cle));
            }
            lignes.add(ligne);
        }

        // Le graphique portait 6 libellés pour 17 valeurs : chaque barre était mal nommée. Un libellé par statut, dans
        // l'ordre du parcours, sans le brouillon (jamais une candidature déposée).
        List<EnumStatutCandidature> statutsCategories = CandidatureService.LIBELLES_STATUTS.keySet().stream()
                .filter(s -> s != EnumStatutCandidature.BROUILLON).toList();
        List<String> categories = statutsCategories.stream().map(CandidatureService.LIBELLES_STATUTS::get).toList();
        List<Double> valeurs = statutsCategories.stream()
                .map(s -> (double) candidatures.stream().filter(c -> c.getStatut() == s).count())
                .toList();

        byte[] fichier = excelExportService.genererClasseur("Candidatures", entetes, lignes,
                "Répartition par statut", categories, valeurs, "Nb candidatures");
        return telechargerXlsx(fichier, "candidatures_recrutement.xlsx");
    }

    /**
     * Télécharge, en une seule archive .zip, l'ensemble des pièces justificatives
     * déposées par les candidatures (filtrées par statut comme l'export Excel) —
     * un dossier par candidature, nommé par son numéro de dossier.
     */
    @GetMapping("/candidatures/documents/export")
    public ResponseEntity<byte[]> exporterDocuments(@RequestParam(required = false) String statut) throws IOException {
        List<CandidatureResponse> candidatures = listerParStatuts(statut);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int nbFichiers = 0;
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (CandidatureResponse c : candidatures) {
                if (c.getPiecesJustificatives() == null || c.getPiecesJustificatives().isEmpty()) continue;
                String dossier = nomDossierZip(c);
                for (PieceJustificativeResponse p : c.getPiecesJustificatives()) {
                    if (p.getFichierStockeId() == null) continue;
                    FichierStocke fichier = fichierStockeService.trouverParId(p.getFichierStockeId());
                    byte[] contenu = fichierStockeService.lireContenu(fichier);
                    String nomFichier = p.getTypeDocument() + "_"
                            + (p.getFichierNomOriginal() != null ? p.getFichierNomOriginal() : ("document_" + p.getId()));
                    zos.putNextEntry(new ZipEntry(dossier + "/" + nomFichier));
                    zos.write(contenu);
                    zos.closeEntry();
                    nbFichiers++;
                }
            }
        }

        if (nbFichiers == 0) {
            return ResponseEntity.badRequest().body(
                    "Aucun document à exporter pour cette sélection.".getBytes(StandardCharsets.UTF_8));
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment().filename("documents_candidatures.zip", StandardCharsets.UTF_8).build());
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/zip")).headers(headers)
                .body(baos.toByteArray());
    }

    /**
     * Rapport qualité recrutement (demande responsable admission) : répartition des
     * candidatures par étape du workflow, avec la liste nominative de chaque étape.
     */
    @GetMapping("/candidatures/rapport/pdf")
    public ResponseEntity<byte[]> genererRapportQualiteRecrutement() {
        RapportRecrutementDto rapport = candidatureService.genererRapportParEtape();
        byte[] pdf = rapportRecrutementPdfService.genererRapport(rapport);
        return telechargerPdf(pdf, "rapport_qualite_recrutement_" + LocalDate.now() + ".pdf");
    }

    /**
     * Candidatures d'un ou de plusieurs statuts ({@code SOUMIS,INCOMPLET}), ou toutes si le filtre est vide : un
     * onglet de l'écran regroupe plusieurs statuts, et son export doit en contenir autant que ce qu'il affiche.
     */
    private List<CandidatureResponse> listerParStatuts(String statuts) {
        if (statuts == null || statuts.isBlank()) {
            return candidatureService.getAllCandidatures();
        }
        java.util.Set<EnumStatutCandidature> voulus = new java.util.HashSet<>();
        for (String brut : statuts.split(",")) {
            try {
                voulus.add(EnumStatutCandidature.valueOf(brut.trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Statut inconnu : « " + brut.trim() + " ».");
            }
        }
        return candidatureService.getAllCandidatures().stream().filter(c -> voulus.contains(c.getStatut())).toList();
    }

    /** Nom de dossier sûr (sans caractères interdits dans un chemin de fichier) pour l'archive de documents. */
    private String nomDossierZip(CandidatureResponse c) {
        String brut = nvl(c.getNumeroDossier()) + "_" + nvl(c.getCandidatNom()) + "_" + nvl(c.getCandidatPrenom());
        return brut.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private Object valeurColonneCandidature(CandidatureResponse c, String cle) {
        return switch (cle) {
            case "numeroDossier" -> nvl(c.getNumeroDossier());
            case "nom" -> nvl(c.getCandidatNom());
            case "prenom" -> nvl(c.getCandidatPrenom());
            case "email" -> nvl(c.getCandidatEmail());
            case "session" -> nvl(c.getSessionCandidatureLibelle());
            case "statut" -> c.getStatut() != null ? CandidatureService.LIBELLES_STATUTS.get(c.getStatut()) : "";
            case "dateSoumission" -> c.getDateSoumission() != null ? c.getDateSoumission().format(DATETIME_FORMAT) : "";
            case "motifRejet" -> nvl(c.getMotifRejet());
            case "genre" -> nvl(c.getCandidatGenre());
            case "niveauEtude" -> nvl(c.getCandidatNiveauEtude());
            case "profession" -> nvl(c.getCandidatProfession());
            case "anneesExperience" -> c.getCandidatAnneesExperience() != null ? c.getCandidatAnneesExperience() : "";
            case "doc_CV" -> statutDocument(c, "CV");
            case "doc_DIPLOME" -> statutDocument(c, "DIPLOME");
            case "doc_RELEVE_NOTES" -> statutDocument(c, "RELEVE_NOTES");
            case "doc_PIECE_IDENTITE" -> statutDocument(c, "PIECE_IDENTITE");
            case "doc_LETTRE_MOTIVATION" -> statutDocument(c, "LETTRE_MOTIVATION");
            default -> "";
        };
    }

    /** Statut d'un type de document précis pour une candidature : Conforme / Non vérifié / Absent. */
    private String statutDocument(CandidatureResponse c, String typeDocument) {
        List<PieceJustificativeResponse> pieces = c.getPiecesJustificatives();
        if (pieces == null) return "Absent";
        return pieces.stream()
                .filter(p -> typeDocument.equals(p.getTypeDocument() != null ? p.getTypeDocument().name() : null))
                .findFirst()
                .map(p -> p.isEstConforme() ? "Conforme" : "Non vérifié")
                .orElse("Absent");
    }

    /**
     * Agent à l'origine de l'action, pour le journal d'étapes (procédure QMS-009).
     * Renvoie {@code null} plutôt que d'échouer : une étape mal attribuée vaut mieux
     * qu'une décision d'admission refusée parce que le journal n'a pas su qui agissait.
     */
    private User agentCourant(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return null;
        }
        User user = userRepository.findByUsername(authentication.getName());
        return user != null ? user : userRepository.findByEmail(authentication.getName());
    }

    private void verifierMotDePasseConfirmation(Authentication authentication, String motDePasse) {
        User agent = agentCourant(authentication);
        if (agent == null || motDePasse == null || !passwordEncoder.matches(motDePasse, agent.getPassword())) {
            throw new BusinessException("Mot de passe de confirmation incorrect.");
        }
    }
    @PutMapping("/candidatures/{id}/valider-preselection")
    public ResponseEntity<CandidatureResponse> validerPreselection(@PathVariable Long id,
            Authentication authentication) {
        CandidatureResponse response = candidatureService.changerStatut(id,
                EnumStatutCandidature.PRESELECTIONNE, null);
        candidatureService.journaliser(id, EnumEtapeQms.FILTRE_INITIAL, response.getStatut(),
                agentCourant(authentication), "Dossier validé et passé en présélection.");
        if (response.getCandidatEmail() != null) {
            notificationService.notifierValidationDossier(response.getCandidatEmail(),
                    response.getNumeroDossier());
        }
        notifierCandidatEnLigne(response, "Votre dossier n°" + response.getNumeroDossier()
                + " a été validé et est en cours de présélection.");
        return ResponseEntity.ok(response);
    }

    /**
     * Met un dossier soumis auquel il manque des pièces en attente du candidat (SOUMIS → INCOMPLET) et lui dit
     * précisément quoi fournir. Le dossier revient seul à l'examen dès qu'il est complet (voir
     * {@code CandidatController#televerserPiece}). C'est l'alternative à la présélection d'un dossier incomplet,
     * que le serveur refuse.
     */
    @PutMapping("/candidatures/{id}/demander-complement")
    public ResponseEntity<CandidatureResponse> demanderComplement(@PathVariable Long id,
            Authentication authentication) {
        CandidatureResponse response = candidatureService.demanderComplement(id);
        List<String> manques = response.getDocumentsAttendus().stream()
                .filter(d -> d.etat() != DocumentAttenduDto.Etat.FOURNI)
                .map(d -> d.libelle() + (d.etat() == DocumentAttenduDto.Etat.A_REMPLACER ? " (à remplacer)" : " (non fourni)"))
                .toList();
        String liste = String.join(", ", manques);
        candidatureService.journaliser(id, EnumEtapeQms.FILTRE_INITIAL, response.getStatut(),
                agentCourant(authentication), "Complément demandé au candidat : " + liste + ".");
        if (response.getCandidatEmail() != null) {
            notificationService.notifierDemandeComplement(response.getCandidatEmail(),
                    response.getNumeroDossier(), liste);
        }
        notifierCandidatEnLigne(response, "Votre dossier n°" + response.getNumeroDossier()
                + " doit être complété : " + liste + ".");
        return ResponseEntity.ok(response);
    }

    /**
     * Enregistre l'issue du test académique d'un candidat convoqué (REUSSI, ECHOUE, ABSENT). Sans ce geste, un
     * dossier convoqué au test ne pouvait ni avancer ni être refusé.
     */
    @PutMapping("/candidatures/{id}/resultat-test")
    public ResponseEntity<CandidatureResponse> enregistrerResultatTest(@PathVariable Long id,
            @RequestBody Map<String, String> body, Authentication authentication) {
        CandidatureResponse response = candidatureService.enregistrerResultatTest(id, body.get("resultat"));
        candidatureService.journaliser(id, EnumEtapeQms.EVALUATION, response.getStatut(),
                agentCourant(authentication), "Résultat du test académique : "
                        + CandidatureService.LIBELLES_STATUTS.get(response.getStatut()) + ".");
        notifierCandidatEnLigne(response, "Le résultat de votre test académique est disponible dans votre espace de suivi.");
        return ResponseEntity.ok(response);
    }

    /** Enregistre l'issue de l'entretien d'un candidat convoqué (EFFECTUE, ABSENT). */
    @PutMapping("/candidatures/{id}/resultat-entretien")
    public ResponseEntity<CandidatureResponse> enregistrerResultatEntretien(@PathVariable Long id,
            @RequestBody Map<String, String> body, Authentication authentication) {
        CandidatureResponse response = candidatureService.enregistrerResultatEntretien(id, body.get("resultat"));
        candidatureService.journaliser(id, EnumEtapeQms.EVALUATION, response.getStatut(),
                agentCourant(authentication), "Entretien : "
                        + CandidatureService.LIBELLES_STATUTS.get(response.getStatut()) + ".");
        notifierCandidatEnLigne(response, "Votre entretien d'admission a été enregistré ; la décision du jury suivra.");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/candidatures/{id}/rejeter")
    public ResponseEntity<CandidatureResponse> rejeterDossier(@PathVariable Long id,
            @RequestBody Map<String, String> body, Authentication authentication) {
        String motif = body.getOrDefault("motif", "Non spécifié");
        // Un candidat déjà admis au test ou à l'entretien qui n'est finalement pas retenu
        // reçoit un message de résultat de parcours plutôt qu'un simple rejet de dossier
        // administratif (TDR 3.1.2/3.1.3 : notification de non-admission après évaluation).
        EnumStatutCandidature statutAvant = candidatureService.getCandidatureById(id).getStatut();
        boolean apresEvaluation = statutAvant == EnumStatutCandidature.CONVOQUE_TEST
                || statutAvant == EnumStatutCandidature.TEST_REUSSI
                || statutAvant == EnumStatutCandidature.TEST_ECHOUE
                || statutAvant == EnumStatutCandidature.CONVOQUE_ENTRETIEN
                || statutAvant == EnumStatutCandidature.ENTRETIEN_EFFECTUE
                || statutAvant == EnumStatutCandidature.ABSENT_TEST
                || statutAvant == EnumStatutCandidature.ABSENT_ENTRETIEN;

        EnumStatutCandidature statutRefus = apresEvaluation
                ? EnumStatutCandidature.NON_ADMIS
                : EnumStatutCandidature.DOSSIER_REJETE;
        CandidatureResponse response = candidatureService.changerStatut(id, statutRefus, motif);
        candidatureService.journaliser(id, EnumEtapeQms.SELECTION, response.getStatut(),
                agentCourant(authentication), "Dossier rejeté. Motif : " + motif);
        if (response.getCandidatEmail() != null) {
            if (apresEvaluation) {
                notificationService.notifierNonAdmission(response.getCandidatEmail(), response.getNumeroDossier());
            } else {
                notificationService.notifierRejetDossier(response.getCandidatEmail(),
                        response.getNumeroDossier(), motif);
            }
        }
        notifierCandidatEnLigne(response, apresEvaluation
                ? "Votre candidature n°" + response.getNumeroDossier() + " n'a malheureusement pas été retenue."
                : "Votre dossier n°" + response.getNumeroDossier() + " a été rejeté. Motif : " + motif);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/candidatures/{id}/admettre-test")
    public ResponseEntity<CandidatureResponse> admettreTestAcademique(@PathVariable Long id,
            Authentication authentication) {
        // TDR : les frais de dossier doivent être réglés avant le passage du test — un
        // candidat qui n'a pas payé ne doit pas pouvoir être convoqué.
        CandidatureResponse existante = candidatureService.getCandidatureById(id);
        PaiementResponse paiement = existante.getPaiement();
        if (paiement == null || paiement.getStatut() != EnumStatutPaiement.SUCCES) {
            throw new BusinessException(
                    "Les frais de dossier doivent être réglés et confirmés avant d'admettre ce candidat au test académique.");
        }

        CandidatureResponse response = candidatureService.changerStatut(id, EnumStatutCandidature.CONVOQUE_TEST,
                null);
        candidatureService.journaliser(id, EnumEtapeQms.EVALUATION, response.getStatut(),
                agentCourant(authentication), "Dossier admis au test académique.");
        if (response.getCandidatEmail() != null) {
            notificationService.notifierAdmissionTest(response.getCandidatEmail(),
                    response.getNumeroDossier());
        }
        notifierCandidatEnLigne(response, "Félicitations ! Votre dossier n°" + response.getNumeroDossier()
                + " est admis au Test Académique.");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/candidatures/{id}/admettre-entretien")
    public ResponseEntity<CandidatureResponse> admettreEntretien(@PathVariable Long id,
            Authentication authentication) {
        CandidatureResponse response = candidatureService.changerStatut(id, EnumStatutCandidature.CONVOQUE_ENTRETIEN,
                null);
        candidatureService.journaliser(id, EnumEtapeQms.EVALUATION, response.getStatut(),
                agentCourant(authentication), "Dossier admis à l'entretien.");
        if (response.getCandidatEmail() != null) {
            notificationService.notifierAdmissionEntretien(response.getCandidatEmail(),
                    response.getNumeroDossier());
        }
        notifierCandidatEnLigne(response, "Félicitations ! Votre dossier n°" + response.getNumeroDossier()
                + " passe à l'étape de l'Entretien.");
        return ResponseEntity.ok(response);
    }

    /**
     * Étape 5 de la procédure QMS-009 : <strong>sélection</strong>. Décide de l'admission,
     * rien de plus.
     *
     * <p>L'ouverture de l'accès étudiant, qui suivait automatiquement, est devenue un geste
     * distinct ({@link #finaliserInscription}) : la procédure qualité en fait l'étape 6,
     * portée par une autre unité. Le risque de cette séparation — un admis oublié, sans
     * accès et sans que personne ne le sache — est tenu par
     * {@link #candidaturesEnAttenteInscription}, la file que le responsable doit vider.
     */
    @PutMapping("/candidatures/{id}/admettre-final")
    public ResponseEntity<CandidatureResponse> admettreFinal(@PathVariable Long id,
            Authentication authentication) {
        CandidatureResponse response = candidatureService.changerStatut(id, EnumStatutCandidature.ADMIS,
                null);
        candidatureService.journaliser(id, EnumEtapeQms.SELECTION, response.getStatut(),
                agentCourant(authentication), "Candidature définitivement admise ; inscription à finaliser.");
        if (response.getCandidatEmail() != null) {
            notificationService.notifierAdmissionFinale(response.getCandidatEmail(),
                    response.getNumeroDossier());
        }
        notifierCandidatEnLigne(response, "Félicitations ! Votre candidature n°" + response.getNumeroDossier()
                + " est définitivement admise à l'INPG.");
        return ResponseEntity.ok(response);
    }

    /**
     * Place un candidat sur liste complémentaire à l'issue de l'entretien (TDR §3.1.C,
     * « saisie des listes retenus et en attente »).
     *
     * <p>Ni une admission ni un rejet : le candidat est retenu sur le fond, mais aucune
     * place n'est disponible. Le frontend affichait déjà ce statut alors que rien ne
     * permettait de l'attribuer — cette action comble ce manque. Le dossier n'en sort
     * que par {@link #admettreFinal} (une place se libère) ou {@link #rejeterDossier}.
     */
    @PutMapping("/candidatures/{id}/liste-attente")
    public ResponseEntity<CandidatureResponse> placerEnListeAttente(@PathVariable Long id,
            Authentication authentication) {
        CandidatureResponse response = candidatureService.changerStatut(id,
                EnumStatutCandidature.LISTE_ATTENTE, null);
        candidatureService.journaliser(id, EnumEtapeQms.SELECTION, response.getStatut(),
                agentCourant(authentication), "Candidat placé sur liste complémentaire.");
        if (response.getCandidatEmail() != null) {
            notificationService.notifierListeAttente(response.getCandidatEmail(), response.getNumeroDossier());
        }
        notifierCandidatEnLigne(response, "Votre candidature n°" + response.getNumeroDossier()
                + " est placée sur liste complémentaire : elle est retenue, en attente qu'une place se libère.");
        return ResponseEntity.ok(response);
    }

    /**
     * Étape 6 de la procédure QMS-009 : <strong>inscription et finalisation</strong>.
     * Ouvre l'accès étudiant au candidat admis, et consigne qui l'a fait.
     *
     * <p>Idempotente : rejouée sur un dossier déjà inscrit, elle ne réécrit rien et
     * n'ajoute pas de seconde ligne au journal.
     */
    @PostMapping("/candidatures/{id}/finaliser-inscription")
    public ResponseEntity<?> finaliserInscription(@PathVariable Long id, Authentication authentication) {
        CandidatureResponse response = candidatureService.getCandidatureById(id);
        if (response.getStatut() != EnumStatutCandidature.ADMIS) {
            return ResponseEntity.badRequest().body(Map.of("message",
                    "Seule une candidature définitivement admise peut être inscrite."));
        }
        if (journalEtapeService.etapeFranchie(id, EnumEtapeQms.INSCRIPTION)) {
            return ResponseEntity.ok(response);
        }
        PaiementResponse paiement = response.getPaiement();
        if (paiement == null || paiement.getStatut() != EnumStatutPaiement.SUCCES) {
            return ResponseEntity.badRequest().body(Map.of("message",
                    "Les frais de dossier doivent être réglés et confirmés avant l'ouverture de l'accès étudiant."));
        }

        inscrireCommeEtudiant(response.getCandidatId());
        response = candidatureService.changerStatut(id, EnumStatutCandidature.INSCRIPTION_FINALISEE, null);
        candidatureService.journaliser(id, EnumEtapeQms.INSCRIPTION, response.getStatut(),
                agentCourant(authentication), "Accès étudiant ouvert ; affectation à une promotion à faire côté pédagogie.");
        return ResponseEntity.ok(response);
    }

    /**
     * Les admis dont l'inscription reste à faire — la file de l'étape 6. Sans elle, la
     * séparation entre décision et inscription laisserait des admis sans accès, exactement
     * le défaut que l'inscription automatique évitait.
     */
    @GetMapping("/candidatures/en-attente-inscription")
    public ResponseEntity<List<CandidatureResponse>> candidaturesEnAttenteInscription() {
        List<CandidatureResponse> enAttente = candidatureService
                .getCandidaturesByStatut(EnumStatutCandidature.ADMIS).stream()
                .filter(c -> !journalEtapeService.etapeFranchie(c.getId(), EnumEtapeQms.INSCRIPTION))
                .collect(Collectors.toList());
        return ResponseEntity.ok(enAttente);
    }

    /** Journal des étapes franchies par un dossier (procédure QMS-009). */
    @GetMapping("/candidatures/{id}/journal")
    public ResponseEntity<List<EtapeDossierResponse>> journalDuDossier(@PathVariable Long id) {
        return ResponseEntity.ok(journalEtapeService.journalDe(id));
    }

    /**
     * Annule la dernière décision prise sur cette candidature (retour au statut
     * précédent). Correction administrative, pas une nouvelle décision : ni email ni
     * notification cloche envoyés, pour ne pas contredire sans explication la
     * notification déjà reçue par le candidat pour la décision qu'on vient d'annuler.
     */
    @PutMapping("/candidatures/{id}/annuler")
    public ResponseEntity<CandidatureResponse> annulerDerniereAction(@PathVariable Long id,
            @RequestBody Map<String, String> body, Authentication authentication) {
        verifierMotDePasseConfirmation(authentication, body.get("motDePasse"));
        return ResponseEntity.ok(candidatureService.annulerDerniereAction(id));
    }

    /**
     * Fait reculer explicitement une candidature vers une étape antérieure choisie,
     * à tout moment — pas seulement juste après la décision à défaire. Une
     * réauthentification protège cette correction sensible.
     */
    @PutMapping("/candidatures/{id}/reculer-statut")
    public ResponseEntity<CandidatureResponse> reculerStatut(@PathVariable Long id,
            @RequestBody Map<String, String> body, Authentication authentication) {
        verifierMotDePasseConfirmation(authentication, body.get("motDePasse"));
        EnumStatutCandidature cible = EnumStatutCandidature.valueOf(body.get("statut").toUpperCase());
        return ResponseEntity.ok(candidatureService.reculerStatut(id, cible));
    }

    /**
     * Ouvre l'accès étudiant à un candidat définitivement admis.
     *
     * <p><strong>Cette méthode n'inscrit pas l'étudiant, elle lui ouvre la porte.</strong>
     * Le compte créé n'a ni promotion ni matricule : c'est au responsable pédagogique,
     * à qui revient la scolarité, de l'affecter — geste qui lui attribue son matricule.
     * D'ici là, l'étudiant apparaît en rouge dans sa liste, marqué « Admission ».
     *
     * <p>Ne pas supprimer cette conversion au motif que la gestion des étudiants
     * appartient au responsable pédagogique : sans elle, un candidat admis resterait
     * avec le seul ROLE_CANDIDAT et n'aurait accès à rien, sans que personne ne sache
     * qu'il attend. La frontière est ailleurs — l'admission ouvre l'accès, la pédagogie
     * inscrit.
     *
     * <p>L'admission finale (ADMIS3) doit donner un accès réel à l'espace étudiant :
     * sans ceci, un candidat admis reste bloqué avec le seul ROLE_CANDIDAT et ne
     * peut pas utiliser le LMS (même bug de fond que Formateur/Etudiant sans
     * profil associé, déjà corrigé côté AdminController#changeUserRoles). Un compte
     * ne peut détenir qu'un seul rôle à la fois : ROLE_CANDIDAT est retiré au profit
     * de ROLE_ETUDIANT lors du passage Candidat -> Étudiant.
     */
    private void inscrireCommeEtudiant(Long candidatId) {
        Candidat candidat = candidatRepository.findById(candidatId).orElse(null);
        if (candidat == null || candidat.getUser() == null) return;
        User user = candidat.getUser();

        boolean dejaEtudiant = user.getRoles().stream().anyMatch(r -> "ROLE_ETUDIANT".equals(r.getCode()));
        if (!dejaEtudiant) {
            Role roleEtudiant = roleRepository.findByCode("ROLE_ETUDIANT");
            if (roleEtudiant != null) {
                user.getRoles().clear();
                user.addRole(roleEtudiant);
                userRepository.save(user);
            }
        }

        if (etudiantRepository.findByUserId(user.getId()).isEmpty()) {
            String telephone = candidat.getTelephone() != null && !candidat.getTelephone().isBlank()
                    ? candidat.getTelephone()
                    : "N/A-" + user.getId();
            etudiantRepository.save(new Etudiant(user, candidat.getNom(), candidat.getPrenom(), telephone));
        }
    }

    /** Notification in-app (cloche candidat) miroir de l'email envoyé à chaque changement de statut. */
    private void notifierCandidatEnLigne(CandidatureResponse response, String message) {
        notificationCandidatService.creerNotification(response.getCandidatId(), message, "STATUT_CHANGE",
                "/recrutement/suivi");
    }

    // ===================== PAIEMENT =====================

    /**
     * Enregistre un paiement effectué hors ligne (espèces au guichet, virement) et le
     * valide directement.
     *
     * <p>C'est le seul moyen de confirmer le règlement d'un candidat qui n'a jamais
     * initié de tentative en ligne (aucun {@code Paiement} n'existe encore) : le
     * couple {@link #simulerSuccesPaiement}/{@link #marquerPaiementEnEchec} n'agit
     * que sur une tentative déjà créée. Fonctionne aussi pour solder une tentative
     * {@code EN_ATTENTE} ou {@code ECHEC} existante sans repasser par le candidat.
     */
    @PostMapping("/candidatures/{candidatureId}/paiement/enregistrer")
    public ResponseEntity<PaiementResponse> enregistrerPaiementHorsLigne(@PathVariable Long candidatureId,
            @RequestBody(required = false) Map<String, String> corps) {
        String methodePaiement = corps != null ? corps.get("methodePaiement") : null;
        return ResponseEntity.ok(paiementService.enregistrerPaiementHorsLigne(candidatureId, methodePaiement));
    }

    /**
     * Confirme manuellement l'encaissement des frais de dossier.
     *
     * <p>Répond à l'exigence du TDR « à l'INPG de confirmer le statut payé le cas
     * échéant » : le versement peut arriver hors ligne (virement, espèces au guichet),
     * ou la notification de l'agrégateur se perdre. L'opération est idempotente — la
     * rejouer sur un paiement déjà validé renvoie le même résultat sans réexpédier les
     * courriels au candidat.
     */
    @PutMapping("/paiements/{id}/simuler-succes")
    public ResponseEntity<PaiementResponse> simulerSuccesPaiement(@PathVariable Long id) {
        return ResponseEntity.ok(paiementService.confirmerPaiement(id));
    }

    /**
     * Solde une tentative de paiement en échec.
     *
     * <p>Le statut ECHEC existait dans l'énumération sans qu'aucun code ne l'affecte
     * jamais : une transaction refusée laissait le dossier indéfiniment EN_ATTENTE, sans
     * que le candidat sache qu'il devait recommencer. Le candidat est notifié et peut
     * relancer un paiement, ce que la règle « un paiement par candidature » interdisait.
     */
    @PutMapping("/paiements/{id}/echec")
    public ResponseEntity<PaiementResponse> marquerPaiementEnEchec(@PathVariable Long id,
            @RequestBody(required = false) Map<String, String> corps) {
        String motif = corps != null ? corps.get("motif") : null;
        return ResponseEntity.ok(paiementService.echouerPaiement(id, motif));
    }

    /** Historique des paiements, toutes candidatures confondues. */
    @GetMapping("/paiements")
    public ResponseEntity<List<PaiementResponse>> getAllPaiements() {
        return ResponseEntity.ok(paiementService.getAllPaiements());
    }

    /**
     * Relance par email + notification les candidats dont le dossier est validé mais dont
     * les frais de dossier restent impayés (TDR : suivi des paiements).
     */
    @PostMapping("/paiements/relance")
    public ResponseEntity<Map<String, Object>> relancerPaiementsImpayes() {
        return ResponseEntity.ok(paiementService.relancerImpayes());
    }

    // ===================== TESTS ACADÉMIQUES =====================

    @GetMapping("/tests")
    public ResponseEntity<List<TestAcademiqueResponse>> getAllTests() {
        return ResponseEntity.ok(testAcademiqueService.getAllTests());
    }

    /** Modèle Excel prérempli pour la saisie ou l'import des notes de test. */
    @GetMapping("/tests/modele-import")
    public ResponseEntity<byte[]> telechargerModeleImportNotes(
            @RequestParam(required = false) Long sessionId) {
        List<List<Object>> lignes = testAcademiqueService.getAllTests().stream()
                .filter(test -> sessionId == null || sessionId.equals(test.getSessionCandidatureId()))
                .map(test -> List.<Object>of(
                        test.getId(),
                        nvl(test.getNomEpreuve()),
                        nvl(test.getSessionCandidatureLibelle()),
                        ""))
                .collect(Collectors.toList());
        return telechargerXlsx(excelExportService.genererClasseur(
                "Notes de tests", List.of("ID test", "Épreuve", "Session", "Note /20"), lignes),
                "modele_import_notes.xlsx");
    }

    /** Export Excel des résultats d'une épreuve, prêt à être partagé avec le jury. */
    @GetMapping("/tests/groupes/{groupeId}/notes/export")
    public ResponseEntity<byte[]> exporterNotesGroupeTest(@PathVariable UUID groupeId) {
        List<TestAcademiqueResponse> tests = testAcademiqueService.getTestsParGroupe(groupeId);
        if (tests.isEmpty()) {
            throw new ResourceNotFoundException("Test académique introuvable.");
        }
        TestAcademiqueResponse epreuve = tests.get(0);
        List<List<Object>> lignes = tests.stream()
                .map(test -> List.<Object>of(
                        test.getCandidatId(),
                        nvl(test.getCandidatNom()),
                        nvl(test.getCandidatPrenom()),
                        test.getNoteObtenue() != null ? test.getNoteObtenue() : ""))
                .collect(Collectors.toList());
        byte[] fichier = excelExportService.genererClasseurAvecTitre(
                "Notes",
                "Relevé des notes — " + nvl(epreuve.getNomEpreuve()),
                List.of("ID candidat", "Nom", "Prénom", "Note"),
                lignes);
        String nomEpreuve = nvl(epreuve.getNomEpreuve()).replaceAll("[^a-zA-Z0-9_-]+", "_");
        return telechargerXlsx(fichier, "releve_notes_" + nomEpreuve + ".xlsx");
    }

    /** Même export depuis une ligne de test : point d'entrée stable pour l'écran de détail. */
    @GetMapping("/tests/{id}/notes/export")
    public ResponseEntity<byte[]> exporterNotesTest(@PathVariable Long id) {
        TestAcademiqueResponse test = testAcademiqueService.getTestById(id);
        return exporterNotesGroupeTest(test.getGroupeEpreuveId());
    }
    /** Lit le modèle .xlsx et n'enregistre le lot que si chaque ligne est valide. */
    @PostMapping("/tests/import-notes")
    public ResponseEntity<Map<String, Object>> importerNotesExcel(
            @RequestParam("fichier") MultipartFile fichier,
            @RequestParam(required = false) Long sessionId) {
        if (fichier.isEmpty()) {
            throw new BusinessException("Sélectionnez un fichier Excel non vide.");
        }
        Map<Long, Float> notes = new LinkedHashMap<>();
        try (Workbook workbook = WorkbookFactory.create(fichier.getInputStream())) {
            Sheet feuille = workbook.getNumberOfSheets() == 0 ? null : workbook.getSheetAt(0);
            if (feuille == null || feuille.getPhysicalNumberOfRows() < 2) {
                throw new BusinessException("Le fichier Excel ne contient aucune ligne de note.");
            }
            DataFormatter format = new DataFormatter();
            for (int index = 1; index <= feuille.getLastRowNum(); index++) {
                Row ligne = feuille.getRow(index);
                if (ligne == null) continue;
                String idBrut = format.formatCellValue(ligne.getCell(0)).trim();
                String noteBrute = format.formatCellValue(ligne.getCell(3)).trim();
                if (idBrut.isEmpty() && noteBrute.isEmpty()) continue;
                if (idBrut.isEmpty() || noteBrute.isEmpty()) {
                    throw new BusinessException("Ligne " + (index + 1) + " : l'identifiant et la note sont requis.");
                }
                long id;
                float note;
                try {
                    id = Long.parseLong(idBrut.replace(".0", ""));
                    note = Float.parseFloat(noteBrute.replace(',', '.'));
                } catch (NumberFormatException exception) {
                    throw new BusinessException("Ligne " + (index + 1) + " : identifiant ou note invalide.");
                }
                if (notes.putIfAbsent(id, note) != null) {
                    throw new BusinessException("Ligne " + (index + 1) + " : le même test apparaît plusieurs fois.");
                }
            }
        } catch (IOException exception) {
            throw new BusinessException("Le fichier Excel ne peut pas être lu.");
        }
        int nombre = testAcademiqueService.importerNotes(notes, sessionId);
        return ResponseEntity.ok(Map.of("count", nombre, "message", nombre + " notes importées."));
    }
    @GetMapping("/tests/{id}")
    public ResponseEntity<TestAcademiqueResponse> getTest(@PathVariable Long id) {
        return ResponseEntity.ok(testAcademiqueService.getTestById(id));
    }

    @PostMapping("/tests")
    public ResponseEntity<TestAcademiqueResponse> creerTest(@RequestBody TestAcademiqueRequest request) {
        return ResponseEntity.ok(testAcademiqueService.creerTest(request));
    }

    @PutMapping("/tests/{id}")
    public ResponseEntity<TestAcademiqueResponse> modifierTest(@PathVariable Long id,
            @RequestBody TestAcademiqueRequest request) {
        return ResponseEntity.ok(testAcademiqueService.modifierTest(id, request));
    }

    /** Saisie rapide de la note seule — utilisé par la grille de notation (édition en ligne). */
    @PutMapping("/tests/{id}/note")
    public ResponseEntity<TestAcademiqueResponse> modifierNoteTest(@PathVariable Long id,
            @RequestBody Map<String, Float> body) {
        return ResponseEntity.ok(testAcademiqueService.modifierNote(id, body.get("noteObtenue")));
    }

    /** Annule la dernière saisie de note d'un test. */
    @PutMapping("/tests/{id}/annuler-note")
    public ResponseEntity<TestAcademiqueResponse> annulerNoteTest(@PathVariable Long id) {
        return ResponseEntity.ok(testAcademiqueService.annulerNote(id));
    }

    @DeleteMapping("/tests/{id}")
    public ResponseEntity<Map<String, String>> supprimerTest(@PathVariable Long id) {
        testAcademiqueService.supprimerTest(id);
        return ResponseEntity.ok(Map.of("message", "Test supprimé avec succès."));
    }

    /** Planifie le même test pour plusieurs candidats à la fois (convocation groupée). */
    @PostMapping("/tests/convocation-groupee")
    public ResponseEntity<List<TestAcademiqueResponse>> convoquerTestsGroupe(
            @RequestBody ConvocationTestGroupeeRequest request) {
        UUID groupeEpreuveId = UUID.randomUUID();
        List<TestAcademiqueResponse> resultats = request.getCandidatIds().stream()
                .map(candidatId -> {
                    TestAcademiqueRequest r = new TestAcademiqueRequest();
                    r.setCandidatId(candidatId);
                    r.setNomEpreuve(request.getNomEpreuve());
                    r.setCoefficient(request.getCoefficient());
                    r.setDateEpreuve(request.getDateEpreuve());
                    r.setLieuOuLien(request.getLieuOuLien());
                    return testAcademiqueService.creerTest(r, groupeEpreuveId);
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(resultats);
    }

    @GetMapping("/tests/groupes/{groupeId}/notes")
    public ResponseEntity<List<TestAcademiqueResponse>> getNotesGroupe(@PathVariable UUID groupeId) {
        return ResponseEntity.ok(testAcademiqueService.getTestsParGroupe(groupeId));
    }

    @PutMapping("/tests/groupes/{groupeId}/notes")
    public ResponseEntity<List<TestAcademiqueResponse>> enregistrerNotesGroupe(@PathVariable UUID groupeId,
            @RequestBody Map<Long, Float> notes) {
        return ResponseEntity.ok(testAcademiqueService.enregistrerNotesGroupe(groupeId, notes));
    }

    @PutMapping("/tests/groupes/{groupeId}")
    public ResponseEntity<List<TestAcademiqueResponse>> modifierGroupeTest(@PathVariable UUID groupeId,
            @RequestBody TestAcademiqueRequest request) {
        return ResponseEntity.ok(testAcademiqueService.modifierGroupe(groupeId, request));
    }

    @DeleteMapping("/tests/groupes/{groupeId}")
    public ResponseEntity<Void> supprimerGroupeTest(@PathVariable UUID groupeId) {
        testAcademiqueService.supprimerGroupe(groupeId);
        return ResponseEntity.noContent().build();
    }

    // ===================== ENTRETIENS =====================

    @GetMapping("/entretiens")
    public ResponseEntity<List<EntretienResponse>> getAllEntretiens() {
        return ResponseEntity.ok(entretienService.getAllEntretiens());
    }

    @GetMapping("/entretiens/{id}")
    public ResponseEntity<EntretienResponse> getEntretien(@PathVariable Long id) {
        return ResponseEntity.ok(entretienService.getEntretienById(id));
    }

    @PostMapping("/entretiens")
    public ResponseEntity<EntretienResponse> creerEntretien(@RequestBody EntretienRequest request) {
        return ResponseEntity.ok(entretienService.creerEntretien(request));
    }

    @PutMapping("/entretiens/{id}")
    public ResponseEntity<EntretienResponse> modifierEntretien(@PathVariable Long id,
            @RequestBody EntretienRequest request) {
        return ResponseEntity.ok(entretienService.modifierEntretien(id, request));
    }

    /** Saisie rapide de la note seule, depuis la liste (grille de notation après entretien). */
    @PutMapping("/entretiens/{id}/note")
    public ResponseEntity<EntretienResponse> modifierNoteEntretien(@PathVariable Long id,
            @RequestBody Map<String, Float> body) {
        return ResponseEntity.ok(entretienService.modifierNote(id, body.get("noteEntretien")));
    }

    @DeleteMapping("/entretiens/{id}")
    public ResponseEntity<Map<String, String>> supprimerEntretien(@PathVariable Long id) {
        entretienService.supprimerEntretien(id);
        return ResponseEntity.ok(Map.of("message", "Entretien supprimé avec succès."));
    }

    /** Planifie le même entretien pour plusieurs candidats à la fois (convocation groupée). */
    @PostMapping("/entretiens/convocation-groupee")
    public ResponseEntity<List<EntretienResponse>> convoquerEntretiensGroupe(
            @RequestBody ConvocationEntretienGroupeeRequest request) {
        UUID groupeEntretienId = UUID.randomUUID();
        List<EntretienResponse> resultats = request.getCandidatIds().stream()
                .map(candidatId -> {
                    EntretienRequest r = new EntretienRequest();
                    r.setCandidatId(candidatId);
                    r.setDateHeure(request.getDateHeure());
                    r.setLieuOuLien(request.getLieuOuLien());
                    return entretienService.creerEntretien(r, groupeEntretienId);
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(resultats);
    }

    @GetMapping("/entretiens/groupes/{groupeId}/notes")
    public ResponseEntity<List<EntretienResponse>> getNotesGroupeEntretien(@PathVariable UUID groupeId) {
        return ResponseEntity.ok(entretienService.getEntretiensParGroupe(groupeId));
    }

    @PutMapping("/entretiens/groupes/{groupeId}/notes")
    public ResponseEntity<List<EntretienResponse>> enregistrerNotesGroupeEntretien(
            @PathVariable UUID groupeId, @RequestBody Map<Long, Float> notes) {
        return ResponseEntity.ok(entretienService.enregistrerNotesGroupe(groupeId, notes));
    }

    @DeleteMapping("/entretiens/groupes/{groupeId}")
    public ResponseEntity<Void> supprimerGroupeEntretien(@PathVariable UUID groupeId) {
        entretienService.supprimerGroupe(groupeId);
        return ResponseEntity.noContent().build();
    }

    // ===================== MODÈLES DE NOTIFICATIONS =====================

    @GetMapping("/notifications/templates")
    public ResponseEntity<List<NotificationTemplate>> getAllTemplates() {
        return ResponseEntity.ok(notificationTemplateService.getAllTemplates());
    }

    @PutMapping("/notifications/templates/{type}")
    public ResponseEntity<NotificationTemplate> modifierTemplate(@PathVariable EnumTypeNotification type,
            @RequestBody NotificationTemplate request) {
        return ResponseEntity.ok(notificationTemplateService.mettreAJour(type, request));
    }

    // ===================== PIÈCES JUSTIFICATIVES (consultation admin) =====================

    @PutMapping("/pieces/{id}/conformite")
    public ResponseEntity<?> modifierConformitePiece(@PathVariable Long id,
            @Validated @RequestBody ModifierConformitePieceRequest body) {
        PieceJustificative piece = pieceJustificativeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pièce justificative non trouvée avec l'ID: " + id));
        boolean conforme = Boolean.TRUE.equals(body.getEstConforme());
        String motif = body.getMotifNonConformite() != null ? body.getMotifNonConformite().trim() : null;
        if (!conforme && (motif == null || motif.isBlank())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Un motif est requis pour demander le remplacement d'un document."));
        }
        piece.setEstConforme(conforme);
        piece.setStatutVerification(conforme
                ? EnumStatutVerificationPiece.CONFORME
                : EnumStatutVerificationPiece.A_REMPLACER);
        piece.setMotifNonConformite(conforme ? null : motif);
        piece.setDateVerification(LocalDateTime.now());
        pieceJustificativeRepository.save(piece);

        Candidature candidature = piece.getCandidature();
        if (candidature != null && candidature.getCandidat() != null) {
            String libelleType = piece.getTypeDocument() != null ? piece.getTypeDocument().libelle() : "Document";
            String numeroDossier = candidature.getNumeroDossier();
            String emailCandidat = candidature.getCandidat().getUser() != null
                    ? candidature.getCandidat().getUser().getEmail()
                    : null;
            if (emailCandidat != null) {
                try {
                    if (conforme) {
                        notificationService.notifierDocumentConforme(emailCandidat, numeroDossier, libelleType);
                    } else {
                        notificationService.notifierDocumentNonConforme(emailCandidat, numeroDossier, libelleType);
                    }
                } catch (Exception e) {
                    log.error("Échec de l'envoi de l'email de conformité de document à {} : {}", emailCandidat,
                            e.getMessage(), e);
                }
            }
            notificationCandidatService.creerNotification(candidature.getCandidat().getId(),
                    conforme
                            ? "Votre document \"" + libelleType + "\" a été vérifié et validé."
                            : "Votre document \"" + libelleType + "\" doit être remplacé : " + motif,
                    "DOCUMENT", "/recrutement/suivi");
        }

        return ResponseEntity.ok(PieceJustificativeResponse.builder()
                .id(piece.getId())
                .typeDocument(piece.getTypeDocument())
                .estConforme(piece.isEstConforme())
                .statutVerification(piece.getStatutVerification())
                .motifNonConformite(piece.getMotifNonConformite())
                .dateVerification(piece.getDateVerification())
                .description(piece.getDescription())
                .fichierStockeId(piece.getFichierStocke() != null ? piece.getFichierStocke().getId() : null)
                .fichierNomOriginal(piece.getFichierStocke() != null ? piece.getFichierStocke().getNomOriginal() : null)
                .moyenneDeclaree(piece.getMoyenneDeclaree())
                .build());
    }

    @GetMapping("/pieces/{id}/telecharger")
    public ResponseEntity<byte[]> telechargerPiece(@PathVariable Long id) {
        PieceJustificative piece = pieceJustificativeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pièce justificative non trouvée avec l'ID: " + id));
        FichierStocke fichier = piece.getFichierStocke();
        if (fichier == null) {
            return ResponseEntity.notFound().build();
        }
        byte[] contenu = fichierStockeService.lireContenu(fichier);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(fichier.getNomOriginal(), StandardCharsets.UTF_8).build());
        MediaType type;
        try {
            type = fichier.getMimeType() != null ? MediaType.parseMediaType(fichier.getMimeType())
                    : MediaType.APPLICATION_OCTET_STREAM;
        } catch (Exception e) {
            type = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok().contentType(type).headers(headers).body(contenu);
    }
}
