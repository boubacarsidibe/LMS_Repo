package inpg.sn.lms.espace_etudiant.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.espace_etudiant.dto.ForumMessageCreateRequest;
import inpg.sn.lms.espace_etudiant.dto.ForumMessageDto;
import inpg.sn.lms.espace_etudiant.dto.ForumSujetCreateRequest;
import inpg.sn.lms.espace_etudiant.dto.ForumSujetDto;
import inpg.sn.lms.espace_etudiant.entity.Cours;
import inpg.sn.lms.espace_etudiant.entity.ForumMessage;
import inpg.sn.lms.espace_etudiant.entity.ForumSujet;
import inpg.sn.lms.espace_etudiant.repository.CoursRepository;
import inpg.sn.lms.espace_etudiant.repository.ForumMessageRepository;
import inpg.sn.lms.espace_etudiant.repository.ForumSujetRepository;
import inpg.sn.lms.espace_etudiant.service.HabilitationCoursService;
import inpg.sn.lms.espace_etudiant.service.MentionForumService;
import inpg.sn.lms.espace_etudiant.service.UtilisateurCourantService;

/**
 * Forum de discussion (TDR §3.2), qui sert aussi de canal de communication
 * apprenant ↔ formateur demandé au §3.5.
 *
 * <p>Un sujet rattaché à un cours n'est visible que des étudiants dont la
 * promotion suit ce cours ; un sujet sans cours est un salon général ouvert à
 * tout l'espace étudiant. Les formateurs voient l'ensemble des sujets : ils
 * encadrent les échanges.
 */
@RestController
@RequestMapping("/api/espace-etudiant/forum")
/*
 * Autorisation rendue explicite : elle ne reposait que sur le prefixe d'URL filtre
 * dans SecurityConfig. Un endpoint ajoute hors de ce prefixe aurait ete ouvert sans
 * que rien ne le signale. Les regles d'URL restent en place et s'appliquent en plus.
 */
@PreAuthorize("hasAnyRole('ETUDIANT','FORMATEUR')")
public class ForumController {

    private final ForumSujetRepository sujetRepository;
    private final ForumMessageRepository messageRepository;
    private final CoursRepository coursRepository;
    private final UtilisateurCourantService utilisateurCourant;
    private final HabilitationCoursService habilitation;
    private final MentionForumService mentions;

    public ForumController(ForumSujetRepository sujetRepository, ForumMessageRepository messageRepository,
            CoursRepository coursRepository, UtilisateurCourantService utilisateurCourant,
            HabilitationCoursService habilitation, MentionForumService mentions) {
        this.sujetRepository = sujetRepository;
        this.messageRepository = messageRepository;
        this.coursRepository = coursRepository;
        this.utilisateurCourant = utilisateurCourant;
        this.habilitation = habilitation;
        this.mentions = mentions;
    }

    /** Sujets accessibles au connecté, épinglés en tête. */
    @GetMapping("/sujets")
    @Transactional(readOnly = true)
    public ResponseEntity<?> sujets(Authentication authentication) {
        return ResponseEntity.ok(sujetsVisibles(authentication).stream().map(this::toSujetDto).toList());
    }

    /** Ouvre un sujet, avec un premier message facultatif. */
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
        Cours cours = null;
        if (request.coursId() != null) {
            cours = coursRepository.findById(request.coursId()).orElse(null);
            if (cours == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Cours introuvable."));
            }
            // Un étudiant ne peut ouvrir un sujet que dans un cours de son cursus.
            Etudiant etudiant = utilisateurCourant.etudiant(authentication);
            if (etudiant != null && !habilitation.estAutorise(cours.getId(), etudiant)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Ce cours ne fait pas partie de votre formation."));
            }
        }
        ForumSujet sujet = sujetRepository.save(new ForumSujet(request.titre().trim(), cours, auteur));
        if (!estVide(request.premierMessage())) {
            messageRepository.save(new ForumMessage(sujet, auteur, request.premierMessage().trim()));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(toSujetDto(sujet));
    }

    /** Fil complet d'un sujet. */
    @GetMapping("/sujets/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> fil(@PathVariable Long id, Authentication authentication) {
        ForumSujet sujet = sujetRepository.findById(id).orElse(null);
        if (sujet == null) {
            return ResponseEntity.notFound().build();
        }
        if (!estVisible(sujet, authentication)) {
            return sujetNonAccessible();
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
        ForumSujet sujet = sujetRepository.findById(id).orElse(null);
        if (sujet == null) {
            return ResponseEntity.notFound().build();
        }
        if (!estVisible(sujet, authentication)) {
            return sujetNonAccessible();
        }
        if (sujet.isClos()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Ce sujet est clos : il n'accepte plus de réponse."));
        }
        if (estVide(request.contenu())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le message ne peut pas être vide."));
        }
        ForumMessage message = messageRepository.save(new ForumMessage(sujet, auteur, request.contenu().trim()));
        mentions.enregistrer(message, request.citeIds(), auteur);
        return ResponseEntity.status(HttpStatus.CREATED).body(toMessageDto(message, auteur));
    }

    /**
     * Personnes citables dans ce fil.
     *
     * <p>Proposer tout l'annuaire reviendrait à publier les noms de toutes les
     * promotions dans une autocomplétion. Ne sont proposées que les personnes qui
     * participent déjà au fil : les étudiants qui y ont accès, et les formateurs du
     * cours. Citer quelqu'un ne lui ouvre aucun droit — la mention suit l'accès.
     */
    @GetMapping("/sujets/{id}/citables")
    @Transactional(readOnly = true)
    public ResponseEntity<?> citables(@PathVariable Long id, Authentication authentication) {
        User demandeur = utilisateurCourant.utilisateur(authentication);
        if (demandeur == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        ForumSujet sujet = sujetRepository.findById(id).orElse(null);
        if (sujet == null) {
            return ResponseEntity.notFound().build();
        }
        if (!estVisible(sujet, authentication)) {
            return sujetNonAccessible();
        }
        return ResponseEntity.ok(mentions.citablesDans(sujet, demandeur));
    }

    /** Supprime son propre message. */
    @DeleteMapping("/messages/{id}")
    @Transactional
    public ResponseEntity<?> supprimerMessage(@PathVariable Long id, Authentication authentication) {
        User utilisateur = utilisateurCourant.utilisateur(authentication);
        ForumMessage message = messageRepository.findById(id).orElse(null);
        if (message == null) {
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

    // ===================== Visibilité =====================

    private List<ForumSujet> sujetsVisibles(Authentication authentication) {
        Etudiant etudiant = utilisateurCourant.etudiant(authentication);
        if (etudiant == null) {
            // Formateur ou personnel : accès à l'ensemble des salons.
            return sujetRepository.findAll();
        }
        List<Long> coursIds = habilitation.idsCoursAutorises(etudiant);
        return coursIds.isEmpty() ? sujetRepository.findGeneraux() : sujetRepository.findVisiblesPour(coursIds);
    }

    private boolean estVisible(ForumSujet sujet, Authentication authentication) {
        if (sujet.getCours() == null) {
            return true;
        }
        Etudiant etudiant = utilisateurCourant.etudiant(authentication);
        if (etudiant == null) {
            return true;
        }
        return habilitation.estAutorise(sujet.getCours().getId(), etudiant);
    }

    // ===================== Correspondances =====================

    private ForumSujetDto toSujetDto(ForumSujet sujet) {
        List<ForumMessage> messages = messageRepository.findBySujetIdOrderByDateCreationAsc(sujet.getId());
        Cours cours = sujet.getCours();
        return new ForumSujetDto(
                sujet.getId(),
                sujet.getTitre(),
                cours == null ? null : cours.getId(),
                cours == null ? null : cours.getTitre(),
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
                estAuteur);
    }

    private ResponseEntity<?> sujetNonAccessible() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Ce sujet ne fait pas partie de vos cours."));
    }

    private boolean estVide(String valeur) {
        return valeur == null || valeur.trim().isEmpty();
    }
}
