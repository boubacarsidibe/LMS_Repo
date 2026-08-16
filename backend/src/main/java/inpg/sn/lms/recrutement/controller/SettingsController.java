package inpg.sn.lms.recrutement.controller;

import inpg.sn.lms.exception.ResourceNotFoundException;
import java.util.*;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import inpg.sn.lms.recrutement.entity.Program;
import inpg.sn.lms.recrutement.repository.ProgramRepository;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final ProgramRepository programRepo;

    public SettingsController(ProgramRepository programRepo) {
        this.programRepo = programRepo;
    }

    // ========== PROGRAMS ==========

    @GetMapping("/programs")
    @PreAuthorize("hasRole('RESPONSABLE_ADMISSION')")
    public ResponseEntity<List<Program>> listPrograms() {
        return ResponseEntity.ok(programRepo.findAll());
    }

    @PostMapping("/programs")
    @PreAuthorize("hasRole('RESPONSABLE_ADMISSION')")
    public ResponseEntity<Program> createProgram(@RequestBody Program program) {
        return ResponseEntity.ok(programRepo.save(program));
    }

    @PutMapping("/programs/{id}")
    @PreAuthorize("hasRole('RESPONSABLE_ADMISSION')")
    public ResponseEntity<Program> updateProgram(@PathVariable Long id, @RequestBody Program body) {
        Program p = programRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Programme introuvable"));
        p.setTitle(body.getTitle());
        p.setLevel(body.getLevel());
        p.setDepartment(body.getDepartment());
        return ResponseEntity.ok(programRepo.save(p));
    }

    @DeleteMapping("/programs/{id}")
    @PreAuthorize("hasRole('RESPONSABLE_ADMISSION')")
    public ResponseEntity<Map<String, String>> deleteProgram(@PathVariable Long id) {
        programRepo.deleteById(id);
        Map<String, String> ok = new HashMap<>();
        ok.put("message", "Programme supprimé");
        return ResponseEntity.ok(ok);
    }

    // ========== RULES ==========

    @GetMapping("/rules")
    @PreAuthorize("hasRole('RESPONSABLE_ADMISSION')")
    public ResponseEntity<Map<String, Object>> getRules() {
        Map<String, Object> rules = new HashMap<>();
        rules.put("seuilTests", 60);
        rules.put("seuilEntretien", 70);
        return ResponseEntity.ok(rules);
    }

    @PutMapping("/rules")
    @PreAuthorize("hasRole('RESPONSABLE_ADMISSION')")
    public ResponseEntity<Map<String, Object>> updateRules(@RequestBody Map<String, Object> body) {
        // Simuler la sauvegarde — les règles sont stockées en session/statiques
        Map<String, Object> rules = new HashMap<>();
        rules.put("seuilTests", body.getOrDefault("seuilTests", 60));
        rules.put("seuilEntretien", body.getOrDefault("seuilEntretien", 70));
        return ResponseEntity.ok(rules);
    }
}