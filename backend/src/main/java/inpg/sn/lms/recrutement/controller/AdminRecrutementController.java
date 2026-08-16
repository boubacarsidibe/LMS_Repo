package inpg.sn.lms.recrutement.controller;

import inpg.sn.lms.exception.ResourceNotFoundException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

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
import inpg.sn.lms.recrutement.dto.EntretienRequest;
import inpg.sn.lms.recrutement.dto.EntretienResponse;
import inpg.sn.lms.recrutement.dto.PaiementResponse;
import inpg.sn.lms.recrutement.dto.PieceJustificativeResponse;
import inpg.sn.lms.recrutement.dto.SessionCandidatureRequest;
import inpg.sn.lms.recrutement.dto.SessionCandidatureResponse;
import inpg.sn.lms.recrutement.dto.TestAcademiqueRequest;
import inpg.sn.lms.recrutement.dto.TestAcademiqueResponse;
import inpg.sn.lms.recrutement.entity.Candidature;
import inpg.sn.lms.recrutement.entity.FichierStocke;
import inpg.sn.lms.recrutement.entity.NotificationTemplate;
import inpg.sn.lms.recrutement.entity.PieceJustificative;
import inpg.sn.lms.recrutement.enums.EnumStatutCandidature;
import inpg.sn.lms.recrutement.enums.EnumTypeDocument;
import inpg.sn.lms.recrutement.enums.EnumTypeNotification;
import inpg.sn.lms.recrutement.repository.PieceJustificativeRepository;
import inpg.sn.lms.recrutement.service.CandidatureService;
import inpg.sn.lms.recrutement.service.EntretienService;
import inpg.sn.lms.recrutement.service.ExcelExportService;
import inpg.sn.lms.recrutement.service.FichierStockeService;
import inpg.sn.lms.recrutement.service.NotificationCandidatService;
import inpg.sn.lms.recrutement.service.NotificationService;
import inpg.sn.lms.recrutement.service.NotificationTemplateService;
import inpg.sn.lms.recrutement.service.PaiementService;
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
            NotificationTemplateService notificationTemplateService) {
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
    }

    private ResponseEntity<byte[]> telechargerXlsx(byte[] contenu, String nomFichier) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(nomFichier, StandardCharsets.UTF_8).build());
        return ResponseEntity.ok().contentType(XLSX).headers(headers).body(contenu);
    }

    private String nvl(String valeur) {
        return valeur == null ? "" : valeur;
    }

    // ===================== SESSIONS =====================

    @GetMapping("/sessions")
    public ResponseEntity<List<SessionCandidatureResponse>> getAllSessions() {
        return ResponseEntity.ok(sessionService.getAllSessions());
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
        List<CandidatureResponse> candidatures = (statut == null || statut.isBlank())
                ? candidatureService.getAllCandidatures()
                : candidatureService.getCandidaturesByStatut(EnumStatutCandidature.valueOf(statut.toUpperCase()));

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

        List<String> categories = List.of("Soumis", "En présélection", "Admis test", "Admis entretien",
                "Admis final", "Rejeté");
        List<EnumStatutCandidature> statutsCategories = List.of(EnumStatutCandidature.SOUMIS,
                EnumStatutCandidature.EN_COURS_DE_PRESELECTION, EnumStatutCandidature.ADMIS1,
                EnumStatutCandidature.ADMIS2, EnumStatutCandidature.ADMIS3, EnumStatutCandidature.REJETE);
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
        List<CandidatureResponse> candidatures = (statut == null || statut.isBlank())
                ? candidatureService.getAllCandidatures()
                : candidatureService.getCandidaturesByStatut(EnumStatutCandidature.valueOf(statut.toUpperCase()));

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
            case "statut" -> c.getStatut() != null ? c.getStatut().name() : "";
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

    @PutMapping("/candidatures/{id}/valider-preselection")
    public ResponseEntity<CandidatureResponse> validerPreselection(@PathVariable Long id) {
        CandidatureResponse response = candidatureService.changerStatut(id,
                EnumStatutCandidature.EN_COURS_DE_PRESELECTION, null);
        if (response.getCandidatEmail() != null) {
            notificationService.notifierValidationDossier(response.getCandidatEmail(),
                    response.getNumeroDossier());
        }
        notifierCandidatEnLigne(response, "Votre dossier n°" + response.getNumeroDossier()
                + " a été validé et est en cours de présélection.");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/candidatures/{id}/rejeter")
    public ResponseEntity<CandidatureResponse> rejeterDossier(@PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String motif = body.getOrDefault("motif", "Non spécifié");
        // Un candidat déjà admis au test ou à l'entretien qui n'est finalement pas retenu
        // reçoit un message de résultat de parcours plutôt qu'un simple rejet de dossier
        // administratif (TDR 3.1.2/3.1.3 : notification de non-admission après évaluation).
        EnumStatutCandidature statutAvant = candidatureService.getCandidatureById(id).getStatut();
        boolean apresEvaluation = statutAvant == EnumStatutCandidature.ADMIS1
                || statutAvant == EnumStatutCandidature.ADMIS2;

        CandidatureResponse response = candidatureService.changerStatut(id, EnumStatutCandidature.REJETE,
                motif);
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
    public ResponseEntity<CandidatureResponse> admettreTestAcademique(@PathVariable Long id) {
        CandidatureResponse response = candidatureService.changerStatut(id, EnumStatutCandidature.ADMIS1,
                null);
        if (response.getCandidatEmail() != null) {
            notificationService.notifierAdmissionTest(response.getCandidatEmail(),
                    response.getNumeroDossier());
        }
        notifierCandidatEnLigne(response, "Félicitations ! Votre dossier n°" + response.getNumeroDossier()
                + " est admis au Test Académique.");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/candidatures/{id}/admettre-entretien")
    public ResponseEntity<CandidatureResponse> admettreEntretien(@PathVariable Long id) {
        CandidatureResponse response = candidatureService.changerStatut(id, EnumStatutCandidature.ADMIS2,
                null);
        if (response.getCandidatEmail() != null) {
            notificationService.notifierAdmissionEntretien(response.getCandidatEmail(),
                    response.getNumeroDossier());
        }
        notifierCandidatEnLigne(response, "Félicitations ! Votre dossier n°" + response.getNumeroDossier()
                + " passe à l'étape de l'Entretien.");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/candidatures/{id}/admettre-final")
    public ResponseEntity<CandidatureResponse> admettreFinal(@PathVariable Long id) {
        CandidatureResponse response = candidatureService.changerStatut(id, EnumStatutCandidature.ADMIS3,
                null);
        if (response.getCandidatEmail() != null) {
            notificationService.notifierAdmissionFinale(response.getCandidatEmail(),
                    response.getNumeroDossier());
        }
        notifierCandidatEnLigne(response, "Félicitations ! Votre candidature n°" + response.getNumeroDossier()
                + " est définitivement admise à l'INPG.");
        inscrireCommeEtudiant(response.getCandidatId());
        return ResponseEntity.ok(response);
    }

    /**
     * L'admission finale (ADMIS3) doit donner un accès réel à l'espace étudiant :
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

    @PutMapping("/paiements/{id}/simuler-succes")
    public ResponseEntity<PaiementResponse> simulerSuccesPaiement(@PathVariable Long id) {
        PaiementResponse response = paiementService.simulerSuccesPaiement(id);
        return ResponseEntity.ok(response);
    }

    /** Historique des paiements, toutes candidatures confondues. */
    @GetMapping("/paiements")
    public ResponseEntity<List<PaiementResponse>> getAllPaiements() {
        return ResponseEntity.ok(paiementService.getAllPaiements());
    }

    // ===================== TESTS ACADÉMIQUES =====================

    @GetMapping("/tests")
    public ResponseEntity<List<TestAcademiqueResponse>> getAllTests() {
        return ResponseEntity.ok(testAcademiqueService.getAllTests());
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

    @DeleteMapping("/tests/{id}")
    public ResponseEntity<Map<String, String>> supprimerTest(@PathVariable Long id) {
        testAcademiqueService.supprimerTest(id);
        return ResponseEntity.ok(Map.of("message", "Test supprimé avec succès."));
    }

    /** Planifie le même test pour plusieurs candidats à la fois (convocation groupée). */
    @PostMapping("/tests/convocation-groupee")
    public ResponseEntity<List<TestAcademiqueResponse>> convoquerTestsGroupe(
            @RequestBody ConvocationTestGroupeeRequest request) {
        List<TestAcademiqueResponse> resultats = request.getCandidatIds().stream()
                .map(candidatId -> {
                    TestAcademiqueRequest r = new TestAcademiqueRequest();
                    r.setCandidatId(candidatId);
                    r.setNomEpreuve(request.getNomEpreuve());
                    r.setCoefficient(request.getCoefficient());
                    r.setDateEpreuve(request.getDateEpreuve());
                    return testAcademiqueService.creerTest(r);
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(resultats);
    }

    // ===================== ENTRETIENS =====================

    @GetMapping("/entretiens")
    public ResponseEntity<List<EntretienResponse>> getAllEntretiens() {
        return ResponseEntity.ok(entretienService.getAllEntretiens());
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

    @DeleteMapping("/entretiens/{id}")
    public ResponseEntity<Map<String, String>> supprimerEntretien(@PathVariable Long id) {
        entretienService.supprimerEntretien(id);
        return ResponseEntity.ok(Map.of("message", "Entretien supprimé avec succès."));
    }

    /** Planifie le même entretien pour plusieurs candidats à la fois (convocation groupée). */
    @PostMapping("/entretiens/convocation-groupee")
    public ResponseEntity<List<EntretienResponse>> convoquerEntretiensGroupe(
            @RequestBody ConvocationEntretienGroupeeRequest request) {
        List<EntretienResponse> resultats = request.getCandidatIds().stream()
                .map(candidatId -> {
                    EntretienRequest r = new EntretienRequest();
                    r.setCandidatId(candidatId);
                    r.setDateHeure(request.getDateHeure());
                    r.setLieuOuLien(request.getLieuOuLien());
                    return entretienService.creerEntretien(r);
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(resultats);
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
    public ResponseEntity<?> modifierConformitePiece(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        PieceJustificative piece = pieceJustificativeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pièce justificative non trouvée avec l'ID: " + id));
        boolean conforme = Boolean.TRUE.equals(body.get("estConforme"));
        piece.setEstConforme(conforme);
        pieceJustificativeRepository.save(piece);

        Candidature candidature = piece.getCandidature();
        if (candidature != null && candidature.getCandidat() != null) {
            String libelleType = libelleTypeDocument(piece.getTypeDocument());
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
                            : "Votre document \"" + libelleType + "\" n'est pas conforme, merci de le remplacer.",
                    "DOCUMENT", "/recrutement/suivi");
        }

        return ResponseEntity.ok(Map.of("id", piece.getId(), "estConforme", piece.isEstConforme()));
    }

    /** Libellé français d'un type de document, réutilisé pour les emails et l'export Excel. */
    private String libelleTypeDocument(EnumTypeDocument type) {
        if (type == null) return "Document";
        return switch (type) {
            case CV -> "CV";
            case DIPLOME -> "Diplôme";
            case RELEVE_NOTES -> "Relevé de notes";
            case PIECE_IDENTITE -> "Pièce d'identité";
            case LETTRE_MOTIVATION -> "Lettre de motivation";
        };
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