package inpg.sn.lms.recrutement.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import inpg.sn.lms.recrutement.dto.CandidatureResponse;
import inpg.sn.lms.recrutement.dto.EntretienRequest;
import inpg.sn.lms.recrutement.dto.EntretienResponse;
import inpg.sn.lms.recrutement.dto.PaiementResponse;
import inpg.sn.lms.recrutement.dto.SessionCandidatureRequest;
import inpg.sn.lms.recrutement.dto.SessionCandidatureResponse;
import inpg.sn.lms.recrutement.dto.TestAcademiqueRequest;
import inpg.sn.lms.recrutement.dto.TestAcademiqueResponse;
import inpg.sn.lms.recrutement.enums.EnumStatutCandidature;
import inpg.sn.lms.recrutement.service.CandidatureService;
import inpg.sn.lms.recrutement.service.EntretienService;
import inpg.sn.lms.recrutement.service.NotificationService;
import inpg.sn.lms.recrutement.service.PaiementService;
import inpg.sn.lms.recrutement.service.SessionCandidatureService;
import inpg.sn.lms.recrutement.service.TestAcademiqueService;

@RestController
@RequestMapping("/api/admin/recrutement")
@CrossOrigin(origins = "http://localhost:4200")
public class AdminRecrutementController {

    private final SessionCandidatureService sessionService;
    private final CandidatureService candidatureService;
    private final PaiementService paiementService;
    private final TestAcademiqueService testAcademiqueService;
    private final EntretienService entretienService;
    private final NotificationService notificationService;

    public AdminRecrutementController(SessionCandidatureService sessionService,
            CandidatureService candidatureService,
            PaiementService paiementService,
            TestAcademiqueService testAcademiqueService,
            EntretienService entretienService,
            NotificationService notificationService) {
        this.sessionService = sessionService;
        this.candidatureService = candidatureService;
        this.paiementService = paiementService;
        this.testAcademiqueService = testAcademiqueService;
        this.entretienService = entretienService;
        this.notificationService = notificationService;
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

    @PutMapping("/candidatures/{id}/valider-preselection")
    public ResponseEntity<CandidatureResponse> validerPreselection(@PathVariable Long id) {
        CandidatureResponse response = candidatureService.changerStatut(id,
                EnumStatutCandidature.EN_COURS_DE_PRESELECTION, null);
        if (response.getCandidatEmail() != null) {
            notificationService.notifierValidationDossier(response.getCandidatEmail(),
                    response.getNumeroDossier());
        }
        return ResponseEntity.ok(response);
    }

    @PutMapping("/candidatures/{id}/rejeter")
    public ResponseEntity<CandidatureResponse> rejeterDossier(@PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String motif = body.getOrDefault("motif", "Non spécifié");
        CandidatureResponse response = candidatureService.changerStatut(id, EnumStatutCandidature.REJETE,
                motif);
        if (response.getCandidatEmail() != null) {
            notificationService.notifierRejetDossier(response.getCandidatEmail(),
                    response.getNumeroDossier(), motif);
        }
        return ResponseEntity.ok(response);
    }

    @PutMapping("/candidatures/{id}/admettre")
    public ResponseEntity<CandidatureResponse> admettreCandidat(@PathVariable Long id) {
        CandidatureResponse response = candidatureService.changerStatut(id, EnumStatutCandidature.ADMIS,
                null);
        if (response.getCandidatEmail() != null) {
            notificationService.notifierAdmission(response.getCandidatEmail(),
                    response.getNumeroDossier());
        }
        return ResponseEntity.ok(response);
    }

    // ===================== PAIEMENT =====================

    @PutMapping("/paiements/{id}/simuler-succes")
    public ResponseEntity<PaiementResponse> simulerSuccesPaiement(@PathVariable Long id) {
        PaiementResponse response = paiementService.simulerSuccesPaiement(id);
        return ResponseEntity.ok(response);
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
}