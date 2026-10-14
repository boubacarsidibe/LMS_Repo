package inpg.sn.lms.recrutement.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
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
                .filter(c -> c.getStatut() == EnumStatutCandidature.TEST_REUSSI
                        || c.getStatut() == EnumStatutCandidature.CONVOQUE_ENTRETIEN
                        || c.getStatut() == EnumStatutCandidature.ENTRETIEN_EFFECTUE
                        || c.getStatut() == EnumStatutCandidature.ADMIS).count());
        dto.setCandidaturesEnAttente(toutes.stream()
                .filter(c -> c.getStatut() == EnumStatutCandidature.SOUMIS
                        || c.getStatut() == EnumStatutCandidature.DOSSIER_VALIDE).count());
        dto.setCandidaturesRejetees(toutes.stream()
                .filter(c -> c.getStatut() == EnumStatutCandidature.DOSSIER_REJETE
                        || c.getStatut() == EnumStatutCandidature.NON_ADMIS).count());

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

    // POST /sessions/new et PATCH /sessions/{id}/close ont été retirés (audit du 2026-10-14) : aucun écran ne les
    // appelait, et la création écrivait le statut « OUVERT », refusé par la base. Les sessions se gèrent par
    // AdminRecrutementController (/api/admin/recrutement/sessions), qui passe par SessionCandidatureService.

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
