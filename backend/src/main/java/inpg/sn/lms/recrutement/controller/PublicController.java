package inpg.sn.lms.recrutement.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import inpg.sn.lms.recrutement.dto.SessionCandidatureResponse;
import inpg.sn.lms.recrutement.service.SessionCandidatureService;

@RestController
@RequestMapping("/api/recrutement/public")
@CrossOrigin(origins = "http://localhost:4200")
public class PublicController {

    private final SessionCandidatureService sessionCandidatureService;

    public PublicController(SessionCandidatureService sessionCandidatureService) {
        this.sessionCandidatureService = sessionCandidatureService;
    }

    /**
     * Consulter le catalogue des sessions ouvertes
     */
    @GetMapping("/catalogue")
    public ResponseEntity<List<SessionCandidatureResponse>> consulterCataloguePublic() {
        List<SessionCandidatureResponse> sessions = sessionCandidatureService.getSessionsOuvertes();
        return ResponseEntity.ok(sessions);
    }

    /**
     * Poser une question (contact)
     */
    @PostMapping("/question")
    public ResponseEntity<Map<String, String>> poserQuestion(@RequestBody Map<String, String> request) {
        String email = request.getOrDefault("email", "");
        String message = request.getOrDefault("message", "");

        if (message.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le message ne peut pas être vide."));
        }

        // TODO: Envoyer l'email au service admission
        return ResponseEntity.ok(Map.of("message", "Votre question a bien été envoyée. Nous vous répondrons dans les plus brefs délais."));
    }
}