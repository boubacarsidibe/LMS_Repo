package inpg.sn.lms.espace_alumni.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.espace_etudiant.dto.ForumMessageCreateRequest;
import inpg.sn.lms.espace_etudiant.dto.ForumMessageDto;
import inpg.sn.lms.espace_etudiant.dto.ForumSujetCreateRequest;
import inpg.sn.lms.espace_etudiant.dto.ForumSujetDto;
import inpg.sn.lms.espace_etudiant.entity.ForumMessage;
import inpg.sn.lms.espace_etudiant.entity.ForumSujet;
import inpg.sn.lms.espace_etudiant.entity.PorteeForumSujet;
import inpg.sn.lms.espace_etudiant.repository.ForumMessageRepository;
import inpg.sn.lms.espace_etudiant.repository.ForumSujetRepository;
import inpg.sn.lms.espace_etudiant.service.MentionForumService;
import inpg.sn.lms.espace_etudiant.service.UtilisateurCourantService;

/**
 * Forum de l'espace Alumni (TDR §C), suite de {@code AlumniController} (profil,
 * situations professionnelles, relevés) laissée de côté à l'ouverture de l'espace
 * alumni faute de notion de portée sur {@link ForumSujet} — voir migration V12 et
 * {@code LOG_BLOQUANT.md}.
 *
 * <p>Réutilise exactement les entités {@code ForumSujet}/{@code ForumMessage}/
 * {@code ForumMention} du forum étudiant plutôt que d'en construire un second
 * système parallèle (cf. CLAUDE.md §3) : un seul salon plat, {@code portee =
 * ALUMNI}, sans notion de cours ni de promotion — un alumni n'en a plus.
 *
 * <p>Vit sous {@code /api/espace-alumni/**}, déjà protégé par {@code hasRole
 * ('ALUMNI')} dans {@code SecurityConfig} ; la double protection par
 * {@code @PreAuthorize} de classe suit le même patron que {@code AlumniController}.
 */
@RestController
@RequestMapping("/api/espace-alumni/forum")
@PreAuthorize("hasRole('ALUMNI')")
public class AlumniForumController {

    private final ForumSujetRepository sujetRepository;
    private final ForumMessageRepository messageRepository;
    private final UtilisateurCourantService utilisateurCourant;
    private final MentionForumService mentions;

    public AlumniForumController(ForumSujetRepository sujetRepository, ForumMessageRepository messageRepository,
            UtilisateurCourantService utilisateurCourant, MentionForumService mentions) {
        this.sujetRepository = sujetRepository;
        this.messageRepository = messageRepository;
        this.utilisateurCourant = utilisateurCourant;
        this.mentions = mentions;
    }

    /** Sujets du salon alumni, les plus récents en tête. */
    @GetMapping("/sujets")
    @Transactional(readOnly = true)
    public ResponseEntity<?> sujets() {
        return ResponseEntity.ok(
                sujetRepository.findByPorteeOrderByEpingleDescDateCreationDesc(PorteeForumSujet.ALUMNI).stream()
                        .map(this::toSujetDto)
                        .toList());
    }

    /** Ouvre un sujet, avec un premier message facultatif. Toujours général : un alumni n'a pas de cours. */
    @PostMapping("/sujets")
    @Transactional
    public ResponseEntity<?> ouvrirSujet(@RequestBody ForumSujetCreateRequest request, Authentication authentication) {
        User auteur = utilisateurCourant.utilisateur(authentication);
        if (auteur == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (estVide(request.titre())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le titre du sujet est obligatoire."));
        }
        ForumSujet sujet = sujetRepository.save(new ForumSujet(request.titre().trim(), auteur, PorteeForumSujet.ALUMNI));
        if (!estVide(request.premierMessage())) {
            messageRepository.save(new ForumMessage(sujet, auteur, request.premierMessage().trim()));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(toSujetDto(sujet));
    }

    /** Fil complet d'un sujet. */
    @GetMapping("/sujets/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> fil(@PathVariable Long id, Authentication authentication) {
        ForumSujet sujet = sujetAlumni(id);
        if (sujet == null) {
            return ResponseEntity.notFound().build();
        }
        User lecteur = utilisateurCourant.utilisateur(authentication);
        List<ForumMessageDto> messages = messageRepository.findBySujetIdOrderByDateCreationAsc(id).stream()
                .map(message -> toMessageDto(message, lecteur))
                .toList();
        return ResponseEntity.ok(Map.of("sujet", toSujetDto(sujet), "messages", messages));
    }

    /** Répond dans un sujet. */
    @PostMapping("/sujets/{id}/messages")
    @Transactional
    public ResponseEntity<?> repondre(@PathVariable Long id, @RequestBody ForumMessageCreateRequest request,
            Authentication authentication) {
        User auteur = utilisateurCourant.utilisateur(authentication);
        if (auteur == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        ForumSujet sujet = sujetAlumni(id);
        if (sujet == null) {
            return ResponseEntity.notFound().build();
        }
        if (estVide(request.contenu())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le message ne peut pas être vide."));
        }
        ForumMessage message = messageRepository.save(new ForumMessage(sujet, auteur, request.contenu().trim()));
        mentions.enregistrer(message, request.citeIds(), auteur);
        return ResponseEntity.status(HttpStatus.CREATED).body(toMessageDto(message, auteur));
    }

    /**
     * Personnes citables dans ce fil : ses participants déjà présents, jamais
     * l'annuaire alumni (cf. javadoc de {@code MentionForumService}).
     */
    @GetMapping("/sujets/{id}/citables")
    @Transactional(readOnly = true)
    public ResponseEntity<?> citables(@PathVariable Long id, Authentication authentication) {
        User demandeur = utilisateurCourant.utilisateur(authentication);
        if (demandeur == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        ForumSujet sujet = sujetAlumni(id);
        if (sujet == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mentions.citablesDans(sujet, demandeur));
    }

    /** Supprime son propre message. */
    @DeleteMapping("/messages/{id}")
    @Transactional
    public ResponseEntity<?> supprimerMessage(@PathVariable Long id, Authentication authentication) {
        User utilisateur = utilisateurCourant.utilisateur(authentication);
        ForumMessage message = messageRepository.findById(id).orElse(null);
        if (message == null || message.getSujet() == null || message.getSujet().getPortee() != PorteeForumSujet.ALUMNI) {
            return ResponseEntity.notFound().build();
        }
        if (utilisateur == null || message.getAuteur() == null
                || !message.getAuteur().getId().equals(utilisateur.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Vous ne pouvez supprimer que vos propres messages."));
        }
        messageRepository.delete(message);
        return ResponseEntity.noContent().build();
    }

    // ===================== Correspondances =====================

    /** Un sujet, seulement s'il appartient bien au salon alumni. */
    private ForumSujet sujetAlumni(Long id) {
        ForumSujet sujet = sujetRepository.findById(id).orElse(null);
        return sujet != null && sujet.getPortee() == PorteeForumSujet.ALUMNI ? sujet : null;
    }

    private ForumSujetDto toSujetDto(ForumSujet sujet) {
        List<ForumMessage> messages = messageRepository.findBySujetIdOrderByDateCreationAsc(sujet.getId());
        return new ForumSujetDto(
                sujet.getId(),
                sujet.getTitre(),
                null,
                null,
                null,
                null,
                utilisateurCourant.nomAffiche(sujet.getAuteur()),
                sujet.getDateCreation(),
                sujet.isEpingle(),
                sujet.isClos(),
                messages.size(),
                messages.isEmpty() ? sujet.getDateCreation() : messages.get(messages.size() - 1).getDateCreation());
    }

    private ForumMessageDto toMessageDto(ForumMessage message, User lecteur) {
        boolean estAuteur = lecteur != null && message.getAuteur() != null
                && message.getAuteur().getId().equals(lecteur.getId());
        return new ForumMessageDto(
                message.getId(),
                utilisateurCourant.nomAffiche(message.getAuteur()),
                message.getContenu(),
                message.getDateCreation(),
                estAuteur,
                null,
                null,
                null,
                null);
    }

    private boolean estVide(String valeur) {
        return valeur == null || valeur.trim().isEmpty();
    }
}
