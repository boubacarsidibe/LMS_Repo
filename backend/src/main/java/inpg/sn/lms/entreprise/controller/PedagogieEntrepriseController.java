package inpg.sn.lms.entreprise.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import inpg.sn.lms.auth.entity.Entreprise;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.EntrepriseRepository;
import inpg.sn.lms.entreprise.dto.MessageEntrepriseDto;
import inpg.sn.lms.entreprise.dto.ReponseDemandeRequest;
import inpg.sn.lms.entreprise.entity.DemandeEntreprise;
import inpg.sn.lms.entreprise.entity.MessageEntreprise;
import inpg.sn.lms.entreprise.enums.EnumStatutDemandeEntreprise;
import inpg.sn.lms.entreprise.repository.DemandeEntrepriseRepository;
import inpg.sn.lms.entreprise.repository.MessageEntrepriseRepository;
import inpg.sn.lms.espace_etudiant.service.UtilisateurCourantService;

/**
 * Volet pédagogique de l'espace entreprise (TDR §H) : consultation de
 * l'ensemble des demandes reçues, réponse et changement de statut,
 * messagerie privée par entreprise.
 *
 * <p>Sous {@code /api/pedagogie/entreprises/**}, distinct des chemins déjà
 * pris par {@code PedagogieController} et {@code PedagogieScolariteController}
 * (voir CLAUDE.md : les deux répondent sur {@code /api/pedagogie}, jamais sur
 * la même route). Couvert par la règle existante
 * {@code /api/pedagogie/** -> hasRole("RESPONSABLE_PEDAGOGIQUE")}.
 */
@RestController
@RequestMapping("/api/pedagogie/entreprises")
public class PedagogieEntrepriseController {

    private final DemandeEntrepriseRepository demandeRepository;
    private final MessageEntrepriseRepository messageRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final UtilisateurCourantService utilisateurCourant;

    public PedagogieEntrepriseController(DemandeEntrepriseRepository demandeRepository,
            MessageEntrepriseRepository messageRepository, EntrepriseRepository entrepriseRepository,
            UtilisateurCourantService utilisateurCourant) {
        this.demandeRepository = demandeRepository;
        this.messageRepository = messageRepository;
        this.entrepriseRepository = entrepriseRepository;
        this.utilisateurCourant = utilisateurCourant;
    }

    /** Toutes les demandes entreprise, tous statuts confondus, les plus récentes d'abord. */
    @GetMapping("/demandes")
    @Transactional(readOnly = true)
    public ResponseEntity<?> demandes() {
        return ResponseEntity.ok(
                demandeRepository.findAllByOrderByDateSoumissionDesc().stream()
                        .map(EntrepriseController::toDto).toList());
    }

    /** Répond à une demande et fait avancer son statut. */
    @PutMapping("/demandes/{id}")
    @Transactional
    public ResponseEntity<?> repondre(@PathVariable Long id, @RequestBody ReponseDemandeRequest requete) {
        DemandeEntreprise demande = demandeRepository.findById(id).orElse(null);
        if (demande == null) {
            return ResponseEntity.notFound().build();
        }
        EnumStatutDemandeEntreprise statut;
        try {
            statut = EnumStatutDemandeEntreprise.valueOf(requete.statut());
        } catch (IllegalArgumentException | NullPointerException invalide) {
            return ResponseEntity.badRequest().body(Map.of("message", "Statut invalide."));
        }
        demande.setStatut(statut);
        if (requete.reponse() != null && !requete.reponse().isBlank()) {
            demande.setReponseEquipe(requete.reponse().trim());
            demande.setDateReponse(LocalDateTime.now());
        }
        demandeRepository.save(demande);
        return ResponseEntity.ok(EntrepriseController.toDto(demande));
    }

    /** Messagerie privée avec une entreprise donnée. */
    @GetMapping("/{entrepriseId}/messages")
    @Transactional(readOnly = true)
    public ResponseEntity<?> messages(@PathVariable Long entrepriseId) {
        Entreprise entreprise = entrepriseRepository.findById(entrepriseId).orElse(null);
        if (entreprise == null) {
            return ResponseEntity.notFound().build();
        }
        List<MessageEntreprise> messages = messageRepository.findByEntrepriseIdOrderByDateCreationAsc(entrepriseId);
        return ResponseEntity.ok(messages.stream().map(m -> EntrepriseController.toMessageDto(m, entreprise)).toList());
    }

    @PostMapping("/{entrepriseId}/messages")
    @Transactional
    public ResponseEntity<?> envoyerMessage(@PathVariable Long entrepriseId, @RequestBody Map<String, String> corps,
            Authentication authentication) {
        Entreprise entreprise = entrepriseRepository.findById(entrepriseId).orElse(null);
        User user = utilisateurCourant.utilisateur(authentication);
        if (entreprise == null || user == null) {
            return ResponseEntity.notFound().build();
        }
        String contenu = corps.get("contenu");
        if (contenu == null || contenu.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le message ne peut pas être vide."));
        }
        MessageEntreprise message = messageRepository.save(new MessageEntreprise(entreprise, user, contenu.trim()));
        MessageEntrepriseDto dto = EntrepriseController.toMessageDto(message, entreprise);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }
}
