package inpg.sn.lms.recrutement.controller;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import inpg.sn.lms.recrutement.entity.Candidature;
import inpg.sn.lms.recrutement.entity.TestAcademique;
import inpg.sn.lms.recrutement.repository.CandidatureRepository;
import inpg.sn.lms.recrutement.repository.TestAcademiqueRepository;

@RestController
@RequestMapping("/api/academic-tests")
public class AcademicTestController {

    private final TestAcademiqueRepository testRepo;
    private final CandidatureRepository candidatureRepo;

    public AcademicTestController(TestAcademiqueRepository testRepo, CandidatureRepository candidatureRepo) {
        this.testRepo = testRepo;
        this.candidatureRepo = candidatureRepo;
    }

    // KPIs
    @GetMapping("/kpis")
    @PreAuthorize("hasRole('RESPONSABLE_ADMISSION')")
    public ResponseEntity<Map<String, Object>> getKpis() {
        List<TestAcademique> all = testRepo.findAll();
        Set<String> epreuves = all.stream().map(TestAcademique::getNomEpreuve).collect(Collectors.toSet());
        long actives = epreuves.size();
        double moyenne = all.stream().filter(t -> t.getNoteObtenue() != null)
                .mapToDouble(TestAcademique::getNoteObtenue).average().orElse(0);
        long notesSaisies = all.stream().filter(t -> t.getNoteObtenue() != null).count();
        double ratio = all.isEmpty() ? 0 : (notesSaisies * 100.0 / all.size());

        Map<String, Object> kpis = new HashMap<>();
        kpis.put("epreuvesActives", actives);
        kpis.put("moyenneGenerale", Math.round(moyenne * 10.0) / 10.0);
        kpis.put("notesSaisiesPct", Math.round(ratio));
        kpis.put("prochainTest", all.stream().filter(t -> t.getDateEpreuve() != null && t.getDateEpreuve().isAfter(LocalDateTime.now()))
                .min(Comparator.comparing(TestAcademique::getDateEpreuve))
                .map(t -> t.getNomEpreuve() + " - " + t.getDateEpreuve().toString()).orElse("N/A"));
        return ResponseEntity.ok(kpis);
    }

    // List exams
    @GetMapping("/exams")
    @PreAuthorize("hasRole('RESPONSABLE_ADMISSION')")
    public ResponseEntity<List<Map<String, Object>>> listExams() {
        Map<String, List<TestAcademique>> grouped = testRepo.findAll().stream()
                .collect(Collectors.groupingBy(TestAcademique::getNomEpreuve));
        List<Map<String, Object>> result = grouped.entrySet().stream().map(e -> {
            Map<String, Object> m = new HashMap<>();
            m.put("nom", e.getKey());
            TestAcademique first = e.getValue().get(0);
            m.put("date", first.getDateEpreuve() != null ? first.getDateEpreuve().toLocalDate().toString() : "");
            m.put("heure", first.getDateEpreuve() != null ? first.getDateEpreuve().toLocalTime().toString() : "");
            m.put("candidats", e.getValue().size());
            m.put("statut", first.getNoteObtenue() != null ? "TERMINE" : "PROGRAMME");
            m.put("coefficient", first.getCoefficient());
            m.put("seuil", 60);
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    // Create exam (batch create TestAcademique entries for all candidats in a session)
    @PostMapping("/exams")
    @PreAuthorize("hasRole('RESPONSABLE_ADMISSION')")
    public ResponseEntity<Map<String, Object>> createExam(@RequestBody Map<String, Object> body) {
        String nom = (String) body.getOrDefault("nomEpreuve", "Nouveau Test");
        float coeff = body.get("coefficient") != null ? ((Number) body.get("coefficient")).floatValue() : 1f;
        LocalDateTime dateEpreuve = body.get("dateEpreuve") != null
                ? LocalDateTime.parse((String) body.get("dateEpreuve"))
                : LocalDateTime.now().plusDays(7);
        Long sessionId = body.get("sessionId") != null ? ((Number) body.get("sessionId")).longValue() : null;
        int seuil = body.get("seuil") != null ? ((Number) body.get("seuil")).intValue() : 60;

        if (sessionId != null) {
            UUID groupeEpreuveId = UUID.randomUUID();
            List<Candidature> candidatures = candidatureRepo.findBySessionCandidatureId(sessionId);
            candidatures.forEach(c -> {
                TestAcademique t = new TestAcademique();
                t.setNomEpreuve(nom);
                t.setCoefficient(coeff);
                t.setDateEpreuve(dateEpreuve);
                t.setGroupeEpreuveId(groupeEpreuveId);
                t.setCandidat(c.getCandidat());
                testRepo.save(t);
            });
        }

        Map<String, Object> result = new HashMap<>();
        result.put("message", "Épreuve créée avec succès");
        result.put("nomEpreuve", nom);
        return ResponseEntity.ok(result);
    }

    // PATCH status
    @PatchMapping("/exams/{nom}/status")
    @PreAuthorize("hasRole('RESPONSABLE_ADMISSION')")
    public ResponseEntity<Map<String, Object>> changeStatus(@PathVariable String nom, @RequestBody Map<String, String> body) {
        String newStatus = body.getOrDefault("status", "TERMINE");
        // For TERMINE, we could trigger auto-calculation of admissibility
        List<TestAcademique> tests = testRepo.findByNomEpreuve(nom);
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Statut changé à " + newStatus);
        result.put("count", tests.size());
        return ResponseEntity.ok(result);
    }

    // GET scores for an exam
    @GetMapping("/exams/{nom}/scores")
    @PreAuthorize("hasRole('RESPONSABLE_ADMISSION')")
    public ResponseEntity<List<Map<String, Object>>> getScores(@PathVariable String nom, @RequestParam(defaultValue = "60") int seuil) {
        List<TestAcademique> tests = testRepo.findByNomEpreuve(nom);
        return ResponseEntity.ok(tests.stream().map(t -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", t.getId());
            m.put("candidatNom", t.getCandidat() != null ? t.getCandidat().getNom() : "");
            m.put("candidatPrenom", t.getCandidat() != null ? t.getCandidat().getPrenom() : "");
            m.put("noteObtenue", t.getNoteObtenue() != null ? t.getNoteObtenue() : 0);
            m.put("coefficient", t.getCoefficient());
            m.put("decision", t.getNoteObtenue() != null && t.getNoteObtenue() >= seuil ? "ADMIS" : "AJOURNE");
            return m;
        }).collect(Collectors.toList()));
    }

    // POST batch scores
    @PostMapping("/exams/{nom}/scores/batch")
    @PreAuthorize("hasRole('RESPONSABLE_ADMISSION')")
    public ResponseEntity<Map<String, Object>> saveScores(@PathVariable String nom, @RequestBody List<Map<String, Object>> scores) {
        int count = 0;
        for (Map<String, Object> s : scores) {
            Long id = s.get("id") != null ? ((Number) s.get("id")).longValue() : null;
            Float note = s.get("noteObtenue") != null ? ((Number) s.get("noteObtenue")).floatValue() : null;
            if (id != null && note != null) {
                TestAcademique t = testRepo.findById(id).orElse(null);
                if (t != null) {
                    t.setNoteObtenue(note);
                    testRepo.save(t);
                    count++;
                }
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("message", count + " notes enregistrées");
        result.put("count", count);
        return ResponseEntity.ok(result);
    }
}
