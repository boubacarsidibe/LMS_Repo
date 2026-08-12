package inpg.sn.lms.recrutement.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import inpg.sn.lms.recrutement.entity.Candidature;
import inpg.sn.lms.recrutement.entity.Entretien;
import inpg.sn.lms.recrutement.repository.CandidatureRepository;
import inpg.sn.lms.recrutement.repository.EntretienRepository;

@RestController
@RequestMapping("/api/interviews")
public class InterviewController {

    private final EntretienRepository entretienRepo;
    private final CandidatureRepository candidatureRepo;

    public InterviewController(EntretienRepository entretienRepo, CandidatureRepository candidatureRepo) {
        this.entretienRepo = entretienRepo;
        this.candidatureRepo = candidatureRepo;
    }

    // ==============================================
    // GET /api/interviews/kpis
    // ==============================================
    @GetMapping("/kpis")
    @PreAuthorize("hasRole('RESPONSABLE_ADMISSION')")
    public ResponseEntity<Map<String, Object>> getKpis() {
        List<Entretien> all = entretienRepo.findAll();
        long total = all.size();
        long termines = all.stream().filter(e -> e.getNoteEntretien() != null).count();
        long aujourdhui = all.stream().filter(e -> e.getDateHeure() != null
                && e.getDateHeure().toLocalDate().equals(LocalDate.now())).count();
        // En attente: candidats admissibles sans entretien
        long enAttente = candidatureRepo.findAll().stream()
                .filter(c -> c.getStatut().name().contains("ADMIS")).count() - total;

        Map<String, Object> kpis = new HashMap<>();
        kpis.put("totalEntretiens", total);
        kpis.put("effectues", termines);
        kpis.put("aujourdhui", aujourdhui);
        kpis.put("enAttente", Math.max(0, enAttente));
        return ResponseEntity.ok(kpis);
    }

    // ==============================================
    // GET /api/interviews (list + filters)
    // ==============================================
    @GetMapping
    @PreAuthorize("hasRole('RESPONSABLE_ADMISSION')")
    public ResponseEntity<List<Map<String, Object>>> listInterviews(
            @RequestParam(defaultValue = "ALL") String tab,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit) {
        List<Entretien> all = entretienRepo.findAll();

        if ("PLANNED".equalsIgnoreCase(tab)) {
            all = all.stream().filter(e -> e.getNoteEntretien() == null).collect(Collectors.toList());
        } else if ("COMPLETED".equalsIgnoreCase(tab)) {
            all = all.stream().filter(e -> e.getNoteEntretien() != null).collect(Collectors.toList());
        }

        if (search != null && !search.isEmpty()) {
            String q = search.toLowerCase();
            all = all.stream().filter(e ->
                (e.getCandidat() != null && e.getCandidat().getNom() != null && e.getCandidat().getNom().toLowerCase().contains(q))
                || (e.getCandidat() != null && e.getCandidat().getPrenom() != null && e.getCandidat().getPrenom().toLowerCase().contains(q))
                || (e.getLieuOuLien() != null && e.getLieuOuLien().toLowerCase().contains(q))
            ).collect(Collectors.toList());
        }

        // Pagination
        int start = page * limit;
        int end = Math.min(start + limit, all.size());
        if (start < all.size()) {
            all = all.subList(start, end);
        } else {
            all = Collections.emptyList();
        }

        return ResponseEntity.ok(all.stream().map(this::toInterviewMap).collect(Collectors.toList()));
    }

    // ==============================================
    // POST /api/interviews
    // ==============================================
    @PostMapping
    @PreAuthorize("hasRole('RESPONSABLE_ADMISSION')")
    public ResponseEntity<Map<String, Object>> createInterview(@RequestBody Map<String, Object> body) {
        Entretien e = new Entretien();
        e.setDateHeure(body.get("dateHeure") != null ? LocalDateTime.parse((String) body.get("dateHeure")) : LocalDateTime.now().plusDays(1));
        e.setLieuOuLien((String) body.getOrDefault("lieuOuLien", "Salle A / Meet"));

        Long candidatId = body.get("candidatId") != null ? ((Number) body.get("candidatId")).longValue() : null;
        if (candidatId != null) {
            Candidature cand = candidatureRepo.findById(candidatId).orElse(null);
            if (cand != null && cand.getCandidat() != null) {
                e.setCandidat(cand.getCandidat());
            }
        }
        e = entretienRepo.save(e);
        return ResponseEntity.ok(toInterviewMap(e));
    }

    // ==============================================
    // PUT /api/interviews/:id
    // ==============================================
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('RESPONSABLE_ADMISSION')")
    public ResponseEntity<Map<String, Object>> updateInterview(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Entretien e = entretienRepo.findById(id).orElseThrow(() -> new RuntimeException("Entretien introuvable"));
        if (body.containsKey("dateHeure")) e.setDateHeure(LocalDateTime.parse((String) body.get("dateHeure")));
        if (body.containsKey("lieuOuLien")) e.setLieuOuLien((String) body.get("lieuOuLien"));
        e = entretienRepo.save(e);
        return ResponseEntity.ok(toInterviewMap(e));
    }

    // ==============================================
    // POST /api/interviews/:id/evaluation
    // ==============================================
    @PostMapping("/{id}/evaluation")
    @PreAuthorize("hasRole('RESPONSABLE_ADMISSION')")
    public ResponseEntity<Map<String, Object>> saveEvaluation(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Entretien e = entretienRepo.findById(id).orElseThrow(() -> new RuntimeException("Entretien introuvable"));
        if (body.containsKey("noteEntretien")) e.setNoteEntretien(((Number) body.get("noteEntretien")).floatValue());
        if (body.containsKey("appreciationGlobale")) e.setAppreciationGlobale((String) body.get("appreciationGlobale"));
        // Calculer le score total
        float note = body.get("noteEntretien") != null ? ((Number) body.get("noteEntretien")).floatValue() : 0f;
        String decision = note >= 60 ? "ADMIS" : note >= 40 ? "LISTE_D_ATTENTE" : "REFUSÉ";
        if (body.containsKey("decision")) decision = (String) body.get("decision");
        e.setNoteEntretien(note);
        e = entretienRepo.save(e);

        Map<String, Object> result = toInterviewMap(e);
        result.put("decision", decision);
        return ResponseEntity.ok(result);
    }

    // ==============================================
    // DELETE /api/interviews/:id
    // ==============================================
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('RESPONSABLE_ADMISSION')")
    public ResponseEntity<Map<String, String>> deleteInterview(@PathVariable Long id) {
        entretienRepo.findById(id).orElseThrow(() -> new RuntimeException("Entretien introuvable"));
        entretienRepo.deleteById(id);
        Map<String, String> ok = new HashMap<>();
        ok.put("message", "Entretien annulé");
        return ResponseEntity.ok(ok);
    }

    // Helper
    private Map<String, Object> toInterviewMap(Entretien e) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", e.getId());
        m.put("dateHeure", e.getDateHeure() != null ? e.getDateHeure().toString() : null);
        m.put("date", e.getDateHeure() != null ? e.getDateHeure().toLocalDate().toString() : "");
        m.put("heure", e.getDateHeure() != null ? e.getDateHeure().toLocalTime().toString().substring(0, 5) : "");
        m.put("lieuOuLien", e.getLieuOuLien());
        m.put("noteEntretien", e.getNoteEntretien());
        m.put("appreciationGlobale", e.getAppreciationGlobale());
        m.put("statut", e.getNoteEntretien() != null ? "TERMINE" : "PLANIFIE");
        if (e.getCandidat() != null) {
            m.put("candidatNom", e.getCandidat().getNom() != null ? e.getCandidat().getNom() : "");
            m.put("candidatPrenom", e.getCandidat().getPrenom() != null ? e.getCandidat().getPrenom() : "");
        }
        return m;
    }
}