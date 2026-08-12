package inpg.sn.lms.recrutement.controller;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import inpg.sn.lms.recrutement.entity.Candidature;
import inpg.sn.lms.recrutement.entity.Program;
import inpg.sn.lms.recrutement.entity.SessionCandidature;
import inpg.sn.lms.recrutement.repository.CandidatureRepository;
import inpg.sn.lms.recrutement.repository.ProgramRepository;
import inpg.sn.lms.recrutement.repository.SessionCandidatureRepository;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionCandidatureRepository sessionRepo;
    private final CandidatureRepository candidatureRepo;
    private final ProgramRepository programRepo;

    public SessionController(SessionCandidatureRepository sessionRepo,
                             CandidatureRepository candidatureRepo,
                             ProgramRepository programRepo) {
        this.sessionRepo = sessionRepo;
        this.candidatureRepo = candidatureRepo;
        this.programRepo = programRepo;
    }

    // ==============================================
    // GET /api/sessions/kpis
    // ==============================================
    @GetMapping("/kpis")
    @PreAuthorize("hasRole('RESPONSABLE_ADMISSION')")
    public ResponseEntity<Map<String, Object>> getKpis() {
        List<SessionCandidature> all = sessionRepo.findAll();
        long totalSessionsActives = all.stream().filter(s -> "OUVERT".equalsIgnoreCase(s.getStatus())).count();
        long totalCandidatures = all.stream().mapToLong(s -> s.getCandidatures().size()).sum();

        long totalQuota = all.stream().mapToLong(SessionCandidature::getQuotaMax).sum();
        long totalInscrits = all.stream().mapToLong(SessionCandidature::getQuotaActuel).sum();
        int tauxRemplissage = totalQuota > 0 ? (int) ((totalInscrits * 100) / totalQuota) : 0;

        Optional<SessionCandidature> prochaineCloture = all.stream()
                .filter(s -> "OUVERT".equalsIgnoreCase(s.getStatus()) && s.getDateFermeture() != null)
                .min((a, b) -> a.getDateFermeture().compareTo(b.getDateFermeture()));

        Map<String, Object> kpis = new HashMap<>();
        kpis.put("totalSessionsActives", totalSessionsActives);
        kpis.put("totalCandidatures", totalCandidatures);
        kpis.put("tauxRemplissage", tauxRemplissage);
        kpis.put("prochaineCloture", prochaineCloture.map(s -> s.getDateFermeture().toString()).orElse("N/A"));

        return ResponseEntity.ok(kpis);
    }

    // ==============================================
    // GET /api/sessions (avec filtres)
    // ==============================================
    @GetMapping
    @PreAuthorize("hasRole('RESPONSABLE_ADMISSION')")
    public ResponseEntity<List<Map<String, Object>>> listSessions(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String academicYear) {

        List<SessionCandidature> sessions = sessionRepo.findAll();

        // Filtre par statut
        if (status != null && !status.isEmpty()) {
            sessions = sessions.stream()
                    .filter(s -> s.getStatus().equalsIgnoreCase(status))
                    .collect(Collectors.toList());
        }

        // Filtre par niveau (via Program.level)
        if (level != null && !level.isEmpty()) {
            sessions = sessions.stream()
                    .filter(s -> s.getProgram() != null && s.getProgram().getLevel().equalsIgnoreCase(level))
                    .collect(Collectors.toList());
        }

        // Filtre par recherche texte (libelle ou program.title)
        if (q != null && !q.isEmpty()) {
            String query = q.toLowerCase();
            sessions = sessions.stream()
                    .filter(s -> s.getLibelle().toLowerCase().contains(query)
                            || (s.getProgram() != null && s.getProgram().getTitle().toLowerCase().contains(query)))
                    .collect(Collectors.toList());
        }

        return ResponseEntity.ok(sessions.stream().map(this::toSessionMap).collect(Collectors.toList()));
    }

    // ==============================================
    // POST /api/sessions
    // ==============================================
    @PostMapping
    @PreAuthorize("hasRole('RESPONSABLE_ADMISSION')")
    public ResponseEntity<Map<String, Object>> createSession(@RequestBody Map<String, Object> body) {
        SessionCandidature session = new SessionCandidature();
        session.setLibelle((String) body.getOrDefault("libelle", "Nouvelle Session"));
        session.setDateOuverture(body.get("dateOuverture") != null
                ? LocalDate.parse((String) body.get("dateOuverture")) : LocalDate.now());
        session.setDateFermeture(body.get("dateFermeture") != null
                ? LocalDate.parse((String) body.get("dateFermeture")) : LocalDate.now().plusMonths(3));
        session.setQuotaMax(body.get("quotaMax") != null ? ((Number) body.get("quotaMax")).intValue() : 220);
        session.setQuotaActuel(0);

        String statut = (String) body.getOrDefault("status", "BROUILLON");
        session.setStatus(statut);
        session.setEstOuverte("OUVERT".equalsIgnoreCase(statut));

        // Program
        if (body.get("programId") != null) {
            Long programId = ((Number) body.get("programId")).longValue();
            programRepo.findById(programId).ifPresent(session::setProgram);
        }

        session = sessionRepo.save(session);
        return ResponseEntity.ok(toSessionMap(session));
    }

    // ==============================================
    // PUT /api/sessions/:id
    // ==============================================
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('RESPONSABLE_ADMISSION')")
    public ResponseEntity<Map<String, Object>> updateSession(@PathVariable Long id,
                                                              @RequestBody Map<String, Object> body) {
        SessionCandidature session = sessionRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Session introuvable"));

        if (body.containsKey("libelle")) session.setLibelle((String) body.get("libelle"));
        if (body.containsKey("dateOuverture"))
            session.setDateOuverture(LocalDate.parse((String) body.get("dateOuverture")));
        if (body.containsKey("dateFermeture"))
            session.setDateFermeture(LocalDate.parse((String) body.get("dateFermeture")));
        if (body.containsKey("quotaMax"))
            session.setQuotaMax(((Number) body.get("quotaMax")).intValue());
        if (body.containsKey("status")) {
            String s = (String) body.get("status");
            session.setStatus(s);
            session.setEstOuverte("OUVERT".equalsIgnoreCase(s));
        }
        if (body.containsKey("programId")) {
            Long programId = ((Number) body.get("programId")).longValue();
            programRepo.findById(programId).ifPresent(session::setProgram);
        }

        session = sessionRepo.save(session);
        return ResponseEntity.ok(toSessionMap(session));
    }

    // ==============================================
    // PATCH /api/sessions/:id/status
    // ==============================================
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('RESPONSABLE_ADMISSION')")
    public ResponseEntity<Map<String, Object>> changeStatus(@PathVariable Long id,
                                                             @RequestBody Map<String, String> body) {
        SessionCandidature session = sessionRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Session introuvable"));
        String newStatus = body.getOrDefault("status", "OUVERT");
        session.setStatus(newStatus);
        session.setEstOuverte("OUVERT".equalsIgnoreCase(newStatus));
        session = sessionRepo.save(session);
        return ResponseEntity.ok(toSessionMap(session));
    }

    // ==============================================
    // DELETE /api/sessions/:id
    // ==============================================
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('RESPONSABLE_ADMISSION')")
    public ResponseEntity<Map<String, String>> deleteSession(@PathVariable Long id) {
        SessionCandidature session = sessionRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Session introuvable"));
        if (!session.getCandidatures().isEmpty()) {
            Map<String, String> err = new HashMap<>();
            err.put("error", "Impossible de supprimer une session avec des candidats inscrits");
            return ResponseEntity.badRequest().body(err);
        }
        sessionRepo.delete(session);
        Map<String, String> ok = new HashMap<>();
        ok.put("message", "Session supprimée");
        return ResponseEntity.ok(ok);
    }

    // ==============================================
    // POST /api/sessions/:id/duplicate
    // ==============================================
    @PostMapping("/{id}/duplicate")
    @PreAuthorize("hasRole('RESPONSABLE_ADMISSION')")
    public ResponseEntity<Map<String, Object>> duplicateSession(@PathVariable Long id) {
        SessionCandidature original = sessionRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Session introuvable"));

        SessionCandidature copy = new SessionCandidature();
        copy.setLibelle(original.getLibelle() + " (Copie)");
        copy.setDateOuverture(LocalDate.now().plusMonths(6));
        copy.setDateFermeture(LocalDate.now().plusMonths(9));
        copy.setEstOuverte(false);
        copy.setQuotaMax(original.getQuotaMax());
        copy.setQuotaActuel(0);
        copy.setStatus("BROUILLON");
        copy.setProgram(original.getProgram());
        copy = sessionRepo.save(copy);
        return ResponseEntity.ok(toSessionMap(copy));
    }

    // ==============================================
    // GET /api/sessions/export
    // ==============================================
    @GetMapping("/export")
    @PreAuthorize("hasRole('RESPONSABLE_ADMISSION')")
    public ResponseEntity<byte[]> exportSessions() {
        List<SessionCandidature> sessions = sessionRepo.findAll();
        StringBuilder csv = new StringBuilder();
        csv.append("Libellé,Programme,Niveau,Quota,Inscrits,Taux,Statut,Début,Fin\n");
        for (SessionCandidature s : sessions) {
            String programTitle = s.getProgram() != null ? s.getProgram().getTitle() : "N/A";
            String level = s.getProgram() != null ? s.getProgram().getLevel() : "N/A";
            int taux = s.getQuotaMax() > 0 ? (s.getQuotaActuel() * 100 / s.getQuotaMax()) : 0;
            csv.append(String.format("\"%s\",\"%s\",\"%s\",%d,%d,%d%%,%s,%s,%s\n",
                    s.getLibelle(), programTitle, level,
                    s.getQuotaMax(), s.getQuotaActuel(), taux, s.getStatus(),
                    s.getDateOuverture(), s.getDateFermeture()));
        }
        byte[] bytes = csv.toString().getBytes();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sessions_export.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(bytes);
    }

    // ==============================================
    // GET /api/programs
    // ==============================================
    @GetMapping("/programs")
    @PreAuthorize("hasRole('RESPONSABLE_ADMISSION')")
    public ResponseEntity<List<Program>> listPrograms() {
        return ResponseEntity.ok(programRepo.findAll());
    }

    // ==============================================
    // Helper
    // ==============================================
    private Map<String, Object> toSessionMap(SessionCandidature s) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", s.getId());
        m.put("libelle", s.getLibelle());
        m.put("dateOuverture", s.getDateOuverture().toString());
        m.put("dateFermeture", s.getDateFermeture().toString());
        m.put("estOuverte", s.isEstOuverte());
        m.put("quotaMax", s.getQuotaMax());
        m.put("quotaActuel", s.getQuotaActuel());
        m.put("status", s.getStatus());
        m.put("nombreCandidatures", s.getCandidatures().size());
        if (s.getProgram() != null) {
            Map<String, Object> prog = new HashMap<>();
            prog.put("id", s.getProgram().getId());
            prog.put("title", s.getProgram().getTitle());
            prog.put("level", s.getProgram().getLevel());
            m.put("program", prog);
        } else {
            m.put("program", null);
        }
        // Quota percentage
        int taux = s.getQuotaMax() > 0 ? (s.getQuotaActuel() * 100 / s.getQuotaMax()) : 0;
        m.put("tauxRemplissage", taux);
        return m;
    }
}