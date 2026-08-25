package inpg.sn.lms.recrutement.controller;

import inpg.sn.lms.exception.ResourceNotFoundException;
import java.util.*;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import inpg.sn.lms.recrutement.entity.Program;
import inpg.sn.lms.recrutement.entity.SessionCandidature;
import inpg.sn.lms.recrutement.repository.ProgramRepository;
import inpg.sn.lms.recrutement.repository.SessionCandidatureRepository;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final ProgramRepository programRepo;
    private final SessionCandidatureRepository sessionRepo;

    public SettingsController(ProgramRepository programRepo, SessionCandidatureRepository sessionRepo) {
        this.programRepo = programRepo;
        this.sessionRepo = sessionRepo;
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

    /**
     * {@code seuilTests} correspond réellement à {@link
     * SessionCandidature#getSeuilReussiteTest()} (note/20, TDR 3.1.2) de la
     * session de candidature actuellement ouverte — avant cette révision, ces
     * deux endpoints renvoyaient une constante (60/70 sur 100) jamais lue nulle
     * part ni persistée (repéré lors d'un audit complet des endpoints).
     *
     * <p>{@code seuilEntretien} reste absent de la réponse : aucune colonne
     * équivalente n'existe pour l'entretien nulle part dans le modèle (
     * {@code InterviewController#saveEvaluation} calcule sa propre décision à
     * partir d'un seuil 60/40 codé en dur, complètement déconnecté de cet
     * endpoint). L'inventer ici serait une décision de modèle — quelle échelle,
     * portée par session ou globale — qui n'appartient pas à cette story ;
     * consigné dans LOG_BLOQUANT.md plutôt que deviné.
     */
    @GetMapping("/rules")
    @PreAuthorize("hasRole('RESPONSABLE_ADMISSION')")
    public ResponseEntity<Map<String, Object>> getRules() {
        Map<String, Object> rules = new HashMap<>();
        SessionCandidature session = sessionRepo.findTopByEstOuverteTrueOrderByIdDesc();
        rules.put("seuilTests", session != null ? session.getSeuilReussiteTest() : null);
        rules.put("sessionId", session != null ? session.getId() : null);
        return ResponseEntity.ok(rules);
    }

    @PutMapping("/rules")
    @PreAuthorize("hasRole('RESPONSABLE_ADMISSION')")
    public ResponseEntity<?> updateRules(@RequestBody Map<String, Object> body) {
        SessionCandidature session = sessionRepo.findTopByEstOuverteTrueOrderByIdDesc();
        if (session == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT)
                    .body(Map.of("message", "Aucune session de candidature n'est actuellement ouverte."));
        }
        Object seuilBrut = body.get("seuilTests");
        if (seuilBrut != null) {
            if (!(seuilBrut instanceof Number)) {
                return ResponseEntity.badRequest().body(Map.of("message", "seuilTests doit être un nombre."));
            }
            session.setSeuilReussiteTest(((Number) seuilBrut).floatValue());
            sessionRepo.save(session);
        }
        Map<String, Object> rules = new HashMap<>();
        rules.put("seuilTests", session.getSeuilReussiteTest());
        rules.put("sessionId", session.getId());
        return ResponseEntity.ok(rules);
    }
}