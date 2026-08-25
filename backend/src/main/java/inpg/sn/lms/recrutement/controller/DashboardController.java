package inpg.sn.lms.recrutement.controller;

import inpg.sn.lms.exception.ResourceNotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import inpg.sn.lms.auth.entity.Candidat;
import inpg.sn.lms.recrutement.dto.DashboardOverviewDTO;
import inpg.sn.lms.recrutement.dto.DashboardOverviewDTO.CandidatureResumeDTO;
import inpg.sn.lms.recrutement.dto.DashboardOverviewDTO.EntretienSummaryDTO;
import inpg.sn.lms.recrutement.dto.DashboardOverviewDTO.SessionSummaryDTO;
import inpg.sn.lms.recrutement.dto.DashboardOverviewDTO.TestSummaryDTO;
import inpg.sn.lms.recrutement.entity.Candidature;
import inpg.sn.lms.recrutement.entity.Entretien;
import inpg.sn.lms.recrutement.entity.SessionCandidature;
import inpg.sn.lms.recrutement.entity.TestAcademique;
import inpg.sn.lms.recrutement.enums.EnumStatutCandidature;
import inpg.sn.lms.recrutement.repository.CandidatureRepository;
import inpg.sn.lms.recrutement.repository.EntretienRepository;
import inpg.sn.lms.recrutement.repository.ProgramRepository;
import inpg.sn.lms.recrutement.repository.SessionCandidatureRepository;
import inpg.sn.lms.recrutement.repository.TestAcademiqueRepository;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final SessionCandidatureRepository sessionRepo;
    private final CandidatureRepository candidatureRepo;
    private final TestAcademiqueRepository testAcademiqueRepo;
    private final EntretienRepository entretienRepo;
    private final ProgramRepository programRepo;

    public DashboardController(
            SessionCandidatureRepository sessionRepo,
            CandidatureRepository candidatureRepo,
            TestAcademiqueRepository testAcademiqueRepo,
            EntretienRepository entretienRepo,
            ProgramRepository programRepo) {
        this.sessionRepo = sessionRepo;
        this.candidatureRepo = candidatureRepo;
        this.testAcademiqueRepo = testAcademiqueRepo;
        this.entretienRepo = entretienRepo;
        this.programRepo = programRepo;
    }

    /**
     * GET /api/dashboard/overview
     * Renvoie toutes les données agrégées de la page Vue d'Ensemble.
     */
    @GetMapping("/overview")
    @PreAuthorize("hasRole('RESPONSABLE_ADMISSION')")
    public ResponseEntity<DashboardOverviewDTO> getOverview() {
        DashboardOverviewDTO dto = new DashboardOverviewDTO();

        // ---- Session active ----
        SessionCandidature sessionActive = sessionRepo.findTopByEstOuverteTrueOrderByIdDesc();
        if (sessionActive != null) {
            SessionSummaryDTO sessionDTO = new SessionSummaryDTO();
            sessionDTO.setId(sessionActive.getId());
            sessionDTO.setLibelle(sessionActive.getLibelle());
            sessionDTO.setEstOuverte(sessionActive.isEstOuverte());
            sessionDTO.setDateOuverture(sessionActive.getDateOuverture());
            sessionDTO.setDateFermeture(sessionActive.getDateFermeture());
            sessionDTO.setQuotaMax(sessionActive.getQuotaMax());
            sessionDTO.setQuotaActuel((int) candidatureRepo.countBySessionCandidatureId(sessionActive.getId()));
            dto.setSessionActive(sessionDTO);
        }

        // ---- KPIs ----
        List<Candidature> toutes = candidatureRepo.findAll();
        dto.setTotalCandidatures(toutes.size());
        dto.setCandidaturesValidees(toutes.stream()
                .filter(c -> c.getStatut() == EnumStatutCandidature.ADMIS1
                        || c.getStatut() == EnumStatutCandidature.ADMIS2
                        || c.getStatut() == EnumStatutCandidature.ADMIS3).count());
        dto.setCandidaturesEnAttente(toutes.stream()
                .filter(c -> c.getStatut() == EnumStatutCandidature.SOUMIS
                        || c.getStatut() == EnumStatutCandidature.EN_COURS_DE_PRESELECTION).count());
        dto.setCandidaturesRejetees(toutes.stream()
                .filter(c -> c.getStatut() == EnumStatutCandidature.REJETE).count());

        // ---- Candidatures récentes ----
        List<Candidature> recentes = candidatureRepo.findTop5ByOrderByDateSoumissionDesc();
        dto.setCandidaturesRecentes(recentes.stream().map(this::toCandidatureResume).collect(Collectors.toList()));

        // ---- Tests ----
        List<TestAcademique> tests = testAcademiqueRepo.findAll();
        dto.setTestsAcademiques(tests.stream().map(t -> {
            TestSummaryDTO ts = new TestSummaryDTO();
            ts.setId(t.getId());
            ts.setNomEpreuve(t.getNomEpreuve());
            ts.setDateEpreuve(t.getDateEpreuve() != null ? t.getDateEpreuve().toLocalDate() : null);
            ts.setNombreCandidats(tests.stream().filter(t2 -> t2.getNomEpreuve().equals(t.getNomEpreuve())).count());
            ts.setSeuilReussite(60);
            return ts;
        }).distinct().collect(Collectors.toList()));

        // ---- Entretiens ----
        List<Entretien> entretiens = entretienRepo.findAll();
        dto.setEntretiens(entretiens.stream().map(e -> {
            EntretienSummaryDTO es = new EntretienSummaryDTO();
            es.setId(e.getId());
            es.setCommission("Commission " + e.getId());
            es.setSalle(e.getLieuOuLien() != null ? e.getLieuOuLien() : "Salle A");
            es.setJury(e.getAppreciationGlobale() != null ? e.getAppreciationGlobale() : "À désigner");
            es.setDateEntretien(e.getDateHeure() != null ? e.getDateHeure().toLocalDate() : null);
            return es;
        }).collect(Collectors.toList()));

        return ResponseEntity.ok(dto);
    }

    /**
     * POST /api/dashboard/sessions/new
     * Crée une nouvelle session de recrutement.
     */
    @PostMapping("/sessions/new")
    @PreAuthorize("hasRole('RESPONSABLE_ADMISSION')")
    public ResponseEntity<SessionSummaryDTO> createSession(@RequestBody Map<String, Object> body) {
        String libelle = (String) body.getOrDefault("libelle", "Nouvelle Session");
        LocalDate dateOuverture = body.get("dateOuverture") != null
                ? LocalDate.parse((String) body.get("dateOuverture"))
                : LocalDate.now();
        LocalDate dateFermeture = body.get("dateFermeture") != null
                ? LocalDate.parse((String) body.get("dateFermeture"))
                : LocalDate.now().plusMonths(3);
        int quotaMax = body.get("quotaMax") != null ? ((Number) body.get("quotaMax")).intValue() : 220;
        String status = (String) body.getOrDefault("status", "OUVERT");

        SessionCandidature session = new SessionCandidature();
        session.setLibelle(libelle);
        session.setDateOuverture(dateOuverture);
        session.setDateFermeture(dateFermeture);
        session.setEstOuverte("OUVERT".equalsIgnoreCase(status));
        session.setQuotaMax(quotaMax);
        session.setStatus(status);

        // Associer un programme si fourni
        if (body.get("programId") != null) {
            Long programId = ((Number) body.get("programId")).longValue();
            programRepo.findById(programId).ifPresent(session::setProgram);
        }

        session = sessionRepo.save(session);

        SessionSummaryDTO dto = new SessionSummaryDTO();
        dto.setId(session.getId());
        dto.setLibelle(session.getLibelle());
        dto.setEstOuverte(session.isEstOuverte());
        dto.setDateOuverture(session.getDateOuverture());
        dto.setDateFermeture(session.getDateFermeture());
        dto.setQuotaMax(session.getQuotaMax());
        dto.setQuotaActuel((int) candidatureRepo.countBySessionCandidatureId(session.getId()));
        return ResponseEntity.ok(dto);
    }

    /**
     * PATCH /api/dashboard/sessions/:id/close
     * Clôture une session de recrutement.
     */
    @PatchMapping("/sessions/{id}/close")
    @PreAuthorize("hasRole('RESPONSABLE_ADMISSION')")
    public ResponseEntity<SessionSummaryDTO> closeSession(@PathVariable Long id) {
        SessionCandidature session = sessionRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session introuvable"));
        session.setEstOuverte(false);
        session = sessionRepo.save(session);

        SessionSummaryDTO dto = new SessionSummaryDTO();
        dto.setId(session.getId());
        dto.setLibelle(session.getLibelle());
        dto.setEstOuverte(session.isEstOuverte());
        dto.setDateOuverture(session.getDateOuverture());
        dto.setDateFermeture(session.getDateFermeture());
        dto.setQuotaMax(session.getQuotaMax());
        dto.setQuotaActuel((int) candidatureRepo.countBySessionCandidatureId(session.getId()));
        return ResponseEntity.ok(dto);
    }

    /**
     * GET /api/dashboard/applications/recent?limit=5
     * Récupère les X dernières candidatures.
     */
    @GetMapping("/applications/recent")
    @PreAuthorize("hasRole('RESPONSABLE_ADMISSION')")
    public ResponseEntity<List<CandidatureResumeDTO>> getRecentApplications(
            @RequestParam(defaultValue = "5") int limit) {
        List<Candidature> recentes = candidatureRepo.findTop5ByOrderByDateSoumissionDesc();
        if (recentes.size() > limit) {
            recentes = recentes.subList(0, limit);
        }
        return ResponseEntity.ok(recentes.stream().map(this::toCandidatureResume).collect(Collectors.toList()));
    }

    /**
     * GET /api/dashboard/applications/search?q=
     * Recherche globale par numéro de dossier, nom ou email.
     */
    @GetMapping("/applications/search")
    @PreAuthorize("hasRole('RESPONSABLE_ADMISSION')")
    public ResponseEntity<List<CandidatureResumeDTO>> searchApplications(@RequestParam String q) {
        List<Candidature> results = candidatureRepo.searchByQuery(q);
        return ResponseEntity.ok(results.stream().map(this::toCandidatureResume).collect(Collectors.toList()));
    }

    // ---- Helper ----

    private CandidatureResumeDTO toCandidatureResume(Candidature c) {
        CandidatureResumeDTO dto = new CandidatureResumeDTO();
        dto.setId(c.getId());
        dto.setNumeroDossier(c.getNumeroDossier());
        Candidat cand = c.getCandidat();
        if (cand != null) {
            dto.setNomCandidat(cand.getNom() != null ? cand.getNom() : "Inconnu");
            dto.setPrenomCandidat(cand.getPrenom() != null ? cand.getPrenom() : "");
            dto.setEmail(cand.getUser() != null ? cand.getUser().getEmail() : "");
        }
        dto.setStatut(c.getStatut().name());
        dto.setDateSoumission(c.getDateSoumission() != null ? c.getDateSoumission().toLocalDate() : null);
        return dto;
    }
}