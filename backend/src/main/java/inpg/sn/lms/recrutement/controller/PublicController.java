package inpg.sn.lms.recrutement.controller;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import inpg.sn.lms.espace_etudiant.entity.Formation;
import inpg.sn.lms.espace_etudiant.repository.FormationRepository;
import inpg.sn.lms.recrutement.dto.FormationDetailPubliqueDto;
import inpg.sn.lms.recrutement.dto.FormationPubliqueDto;
import inpg.sn.lms.recrutement.dto.SessionCandidatureResponse;
import inpg.sn.lms.recrutement.service.NotificationService;
import inpg.sn.lms.recrutement.service.SessionCandidatureService;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/recrutement/public")
@CrossOrigin(origins = "http://localhost:4200")
public class PublicController {

    private static final Logger log = LoggerFactory.getLogger(PublicController.class);

    private final SessionCandidatureService sessionCandidatureService;
    private final NotificationService notificationService;
    private final FormationRepository formationRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.admission.email:admin@inpg.sn}")
    private String emailAdmission;

    public PublicController(SessionCandidatureService sessionCandidatureService,
                             NotificationService notificationService,
                             FormationRepository formationRepository,
                             ObjectMapper objectMapper) {
        this.sessionCandidatureService = sessionCandidatureService;
        this.notificationService = notificationService;
        this.formationRepository = formationRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Catalogue public des formations.
     *
     * <p>Ces données vivaient dans deux tableaux TypeScript figés au cœur du
     * front ; elles proviennent désormais de la base et suivent ce que le service
     * pédagogique y publie.
     */
    @GetMapping("/formations")
    public ResponseEntity<List<FormationPubliqueDto>> consulterFormations() {
        return ResponseEntity.ok(formationRepository.findByPublieeTrueOrderByIntituleAsc().stream()
                .map(this::toFormationDto)
                .toList());
    }

    /** Fiche détaillée d'une formation publiée. */
    @GetMapping("/formations/{id}")
    public ResponseEntity<?> consulterFormation(@PathVariable Long id) {
        Formation formation = formationRepository.findById(id).orElse(null);
        // Une formation non publiée reste invisible du public : on ne distingue
        // pas « inexistante » de « non publiée ».
        if (formation == null || !formation.isPubliee()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new FormationDetailPubliqueDto(
                formation.getId(),
                formation.getCode(),
                formation.getIntitule(),
                formation.getDescription(),
                formation.getNiveau(),
                formation.getDomaine(),
                formation.getDuree(),
                formation.getImageUrl(),
                lireFiche(formation)));
    }

    private FormationPubliqueDto toFormationDto(Formation formation) {
        return new FormationPubliqueDto(
                formation.getId(),
                formation.getCode(),
                formation.getIntitule(),
                formation.getDescription(),
                formation.getNiveau(),
                formation.getDomaine(),
                formation.getDuree(),
                formation.getImageUrl());
    }

    /** Renvoie la fiche éditoriale, ou {@code null} si elle est absente ou illisible. */
    private JsonNode lireFiche(Formation formation) {
        String fiche = formation.getFicheDetaillee();
        if (fiche == null || fiche.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(fiche);
        } catch (JsonProcessingException e) {
            log.warn("Fiche détaillée illisible pour la formation {} : {}", formation.getCode(), e.getMessage());
            return null;
        }
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
        // getOrDefault renvoie null si la clé est présente avec une valeur nulle.
        String email = request.get("email") == null ? "" : request.get("email").trim();
        String message = request.get("message") == null ? "" : request.get("message").trim();

        if (message.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le message ne peut pas être vide."));
        }
        if (email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "L'adresse email est requise pour vous répondre."));
        }

        // Envoi de l'email au service admission avec l'email du visiteur en objet,
        // afin que le responsable admission puisse identifier et répondre facilement.
        try {
            notificationService.envoyerEmail(
                    emailAdmission,
                    "Nouvelle question du site (" + email + ")",
                    "Une nouvelle question a été posée depuis le site public.\n\n"
                            + "Email du visiteur : " + email + "\n\n"
                            + "Message :\n" + message
            );
        } catch (Exception e) {
            // On ne fait pas échouer la requête du visiteur si le serveur SMTP est
            // momentanément indisponible : on journalise l'incident pour investigation
            // et on informe tout de même l'utilisateur que sa demande a bien été reçue.
            log.error("Échec de l'envoi de l'email de contact vers {} : {}", emailAdmission, e.getMessage(), e);
        }

        return ResponseEntity.ok(Map.of("message", "Votre question a bien été envoyée. Nous vous répondrons dans les plus brefs délais."));
    }
}