package inpg.sn.lms.espace_etudiant.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;

import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.espace_etudiant.dto.ForumMessageDto;
import inpg.sn.lms.espace_etudiant.dto.ForumSujetCreateRequest;
import inpg.sn.lms.espace_etudiant.dto.ForumSujetDto;
import inpg.sn.lms.espace_etudiant.entity.Cours;
import inpg.sn.lms.espace_etudiant.entity.ForumMessage;
import inpg.sn.lms.espace_etudiant.entity.ForumSujet;
import inpg.sn.lms.espace_etudiant.entity.Notification;
import inpg.sn.lms.espace_etudiant.entity.Promotion;
import inpg.sn.lms.espace_etudiant.repository.CoursRepository;
import inpg.sn.lms.espace_etudiant.repository.ForumMessageRepository;
import inpg.sn.lms.espace_etudiant.repository.ForumSujetRepository;
import inpg.sn.lms.espace_etudiant.repository.NotificationRepository;
import inpg.sn.lms.espace_etudiant.service.ForumVisibiliteService;
import inpg.sn.lms.espace_etudiant.service.HabilitationCoursService;
import inpg.sn.lms.espace_etudiant.service.MentionForumService;
import inpg.sn.lms.espace_etudiant.service.PerimetreFormateurService;
import inpg.sn.lms.espace_etudiant.service.UtilisateurCourantService;
import inpg.sn.lms.auth.entity.Formateur;
import inpg.sn.lms.fichier.ValidationFichier;

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
    private final ForumVisibiliteService visibilite;
    private final NotificationRepository notificationRepository;
    private final PerimetreFormateurService perimetreFormateur;
    private final Path uploadDirectory;

    public ForumController(ForumSujetRepository sujetRepository, ForumMessageRepository messageRepository,
            CoursRepository coursRepository, UtilisateurCourantService utilisateurCourant,
            HabilitationCoursService habilitation, MentionForumService mentions, ForumVisibiliteService visibilite,
            NotificationRepository notificationRepository, PerimetreFormateurService perimetreFormateur,
            @Value("${app.upload.dir:uploads}") String uploadDirectory) {
        this.sujetRepository = sujetRepository;
        this.messageRepository = messageRepository;
        this.coursRepository = coursRepository;
        this.utilisateurCourant = utilisateurCourant;
        this.habilitation = habilitation;
        this.mentions = mentions;
        this.visibilite = visibilite;
        this.notificationRepository = notificationRepository;
        this.perimetreFormateur = perimetreFormateur;
        // Même dossier « messages » que EspaceEtudiantController#repondreAuSujet :
        // les deux contrôleurs posent des réponses sur la même table forum_messages,
        // la pièce jointe doit donc se retrouver au même endroit quel que soit celui
        // qui a servi la requête.
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

    /** Sujets accessibles au connecté, épinglés en tête. */
    @GetMapping("/sujets")
    @Transactional(readOnly = true)
    public ResponseEntity<?> sujets(Authentication authentication) {
        return ResponseEntity.ok(visibilite.sujetsVisibles(authentication).stream().map(this::toSujetDto).toList());
    }

    /**
     * Ouvre un sujet, avec un premier message facultatif.
     *
     * <p>{@code request.coursId()} et {@code request.pourMaPromotion()} sont
     * mutuellement exclusifs. Dans le second cas, la promotion utilisée est
     * toujours celle de l'auteur, lue côté serveur — jamais un identifiant fourni
     * par le client, qui pourrait sinon rattacher un sujet à la promotion d'un
     * tiers.
     */
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
        if (request.coursId() != null && request.pourMaPromotion()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Un sujet ne peut être rattaché à la fois à un cours et à une promotion."));
        }

        ForumSujet sujet;
        if (request.pourMaPromotion()) {
            Etudiant etudiant = utilisateurCourant.etudiant(authentication);
            Promotion promotion = etudiant == null ? null : etudiant.getPromotion();
            if (promotion == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Vous n'êtes rattaché à aucune promotion."));
            }
            sujet = sujetRepository.save(new ForumSujet(request.titre().trim(), promotion, auteur));
        } else {
            Cours cours = null;
            if (request.coursId() != null) {
                cours = coursRepository.findById(request.coursId()).orElse(null);
                if (cours == null) {
                    return ResponseEntity.badRequest().body(Map.of("message", "Cours introuvable."));
                }
                // Un étudiant ne peut ouvrir un sujet que dans un cours de son cursus ; un
                // formateur, que dans un cours qu'il enseigne — sans ce second contrôle,
                // n'importe quel formateur aurait pu adresser un sujet aux étudiants d'un
                // cours qui n'est pas le sien (le cloisonnement `cours_formateur` du module,
                // voir CLAUDE.md §3, n'était jusqu'ici vérifié que côté étudiant ici).
                Etudiant etudiant = utilisateurCourant.etudiant(authentication);
                if (etudiant != null && !habilitation.estAutorise(cours.getId(), etudiant)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("message", "Ce cours ne fait pas partie de votre formation."));
                }
                Formateur formateur = utilisateurCourant.formateur(authentication);
                if (formateur != null && !perimetreFormateur.estFormateurDuCours(formateur, cours)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("message", "Vous n'enseignez pas ce cours."));
                }
            }
            sujet = sujetRepository.save(new ForumSujet(request.titre().trim(), cours, auteur));
        }
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
        if (!visibilite.estVisible(sujet, authentication)) {
            return sujetNonAccessible();
        }
        User lecteur = utilisateurCourant.utilisateur(authentication);
        List<ForumMessageDto> messages = messageRepository.findBySujetIdOrderByDateCreationAsc(id).stream()
                .map(message -> toMessageDto(message, lecteur))
                .toList();
        return ResponseEntity.ok(Map.of("sujet", toSujetDto(sujet), "messages", messages));
    }

    /**
     * Répond dans un sujet, avec une pièce jointe facultative.
     *
     * <p>Passé en multipart (et non plus en JSON) pour porter ce fichier — jusqu'ici
     * seul {@code EspaceEtudiantController#repondreAuSujet} le pouvait, ce qui
     * privait les étudiants de la capacité d'en joindre un depuis leur propre écran
     * de forum ({@code fil-forum.ts}), alors que le formateur le peut depuis le
     * sien. Les deux contrôleurs écrivent dans la même table {@code forum_messages}
     * : une pièce jointe posée ici se télécharge par le même endpoint partagé,
     * {@code GET /sujets/messages/{id}/piece-jointe}.
     */
    @PostMapping(value = "/sujets/{id}/messages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<?> repondre(@PathVariable Long id,
            @RequestParam String contenu,
            @RequestParam(required = false) List<Long> citeIds,
            @RequestParam(required = false) Long reponseAId,
            @RequestParam(value = "fichier", required = false) MultipartFile fichier,
            Authentication authentication) {
        User auteur = utilisateurCourant.utilisateur(authentication);
        if (auteur == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        ForumSujet sujet = sujetRepository.findById(id).orElse(null);
        if (sujet == null) {
            return ResponseEntity.notFound().build();
        }
        if (!visibilite.estVisible(sujet, authentication)) {
            return sujetNonAccessible();
        }
        if (sujet.isClos()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Ce sujet est clos : il n'accepte plus de réponse."));
        }
        if (estVide(contenu)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le message ne peut pas être vide."));
        }
        ForumMessage reponseA = null;
        if (reponseAId != null) {
            // Recoupé avec le sujet courant : un identifiant d'un autre fil ne doit pas
            // pouvoir être glissé pour citer un message auquel l'auteur n'a pas accès.
            reponseA = messageRepository.findById(reponseAId)
                    .filter(candidat -> candidat.getSujet().getId().equals(sujet.getId()))
                    .orElse(null);
        }

        String pieceJointeNomOriginal = null;
        String pieceJointeNomStocke = null;
        String pieceJointeTypeContenu = null;
        if (fichier != null && !fichier.isEmpty()) {
            try {
                ValidationFichier.exiger(fichier,
                        ValidationFichier.EXTENSIONS_SUPPORT_COURS,
                        ValidationFichier.TYPES_SUPPORT_COURS,
                        "PDF, Word, PowerPoint, Excel, images, archives ZIP, audio et vidéo");
            } catch (IllegalArgumentException invalide) {
                return ResponseEntity.badRequest().body(Map.of("message", invalide.getMessage()));
            }
            pieceJointeNomOriginal = ValidationFichier.nomSur(fichier, "fichier");
            pieceJointeNomStocke = UUID.randomUUID() + "-" + pieceJointeNomOriginal;
            pieceJointeTypeContenu = fichier.getContentType();
            try {
                Path messagesDirectory = uploadDirectory.resolve("messages");
                Files.createDirectories(messagesDirectory);
                Files.copy(fichier.getInputStream(),
                        ValidationFichier.destinationSure(messagesDirectory, pieceJointeNomStocke),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException exception) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Le fichier n'a pas pu être enregistré."));
            }
        }

        ForumMessage message = new ForumMessage(sujet, auteur, contenu.trim());
        message.setReponseA(reponseA);
        message.setPieceJointeNomOriginal(pieceJointeNomOriginal);
        message.setPieceJointeNomStocke(pieceJointeNomStocke);
        message.setPieceJointeTypeContenu(pieceJointeTypeContenu);
        message = messageRepository.save(message);
        mentions.enregistrer(message, citeIds == null ? List.of() : citeIds, auteur);
        notifierReponse(message, reponseA, auteur);
        return ResponseEntity.status(HttpStatus.CREATED).body(toMessageDto(message, auteur));
    }

    /**
     * Prévient l'auteur du message cité, comme dans WhatsApp. Aucune notification si
     * on se répond à soi-même. Indépendante des citations explicites
     * ({@code citeIds}) : répondre à un message et y citer quelqu'un d'autre sont deux
     * intentions distinctes, chacune notifiée pour ce qu'elle est.
     */
    private void notifierReponse(ForumMessage message, ForumMessage reponseA, User auteur) {
        if (reponseA == null || reponseA.getAuteur() == null) {
            return;
        }
        User destinataire = reponseA.getAuteur();
        if (destinataire.getId().equals(auteur.getId())) {
            return;
        }
        notificationRepository.save(new Notification(
                destinataire,
                utilisateurCourant.nomAffiche(auteur) + " a répondu à votre message dans « "
                        + message.getSujet().getTitre() + " »",
                Notification.REPONSE_FORUM,
                "/espace-etudiant/forum/" + message.getSujet().getId()));
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
        if (!visibilite.estVisible(sujet, authentication)) {
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

    // ===================== Correspondances =====================

    private ForumSujetDto toSujetDto(ForumSujet sujet) {
        List<ForumMessage> messages = messageRepository.findBySujetIdOrderByDateCreationAsc(sujet.getId());
        Cours cours = sujet.getCours();
        Promotion promotion = sujet.getPromotion();
        return new ForumSujetDto(
                sujet.getId(),
                sujet.getTitre(),
                cours == null ? null : cours.getId(),
                cours == null ? null : cours.getTitre(),
                promotion == null ? null : promotion.getId(),
                promotion == null ? null : promotion.getLibelle(),
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
        ForumMessage reponseA = message.getReponseA();
        return new ForumMessageDto(
                message.getId(),
                utilisateurCourant.nomAffiche(message.getAuteur()),
                message.getContenu(),
                message.getDateCreation(),
                estAuteur,
                reponseA == null ? null : reponseA.getId(),
                reponseA == null ? null : utilisateurCourant.nomAffiche(reponseA.getAuteur()),
                reponseA == null ? null : extrait(reponseA.getContenu()),
                pieceJointeDisponible(message) ? message.getPieceJointeNomOriginal() : null);
    }

    /**
     * Même contrôle que {@code EspaceEtudiantController#pieceJointeDisponible} :
     * un message ancien peut annoncer un fichier que le disque n'a plus (voir ce
     * commentaire pour le détail). Dupliqué plutôt que partagé par souci de ne pas
     * faire dépendre ce contrôleur de l'autre pour trois lignes.
     */
    private boolean pieceJointeDisponible(ForumMessage message) {
        String nomStocke = message.getPieceJointeNomStocke();
        if (nomStocke == null) {
            return false;
        }
        Path chemin = uploadDirectory.resolve("messages").resolve(nomStocke).normalize();
        return chemin.startsWith(uploadDirectory) && Files.isRegularFile(chemin);
    }

    /** Court résumé du message cité, affiché au-dessus de la réponse (façon WhatsApp). */
    private String extrait(String contenu) {
        if (contenu == null) {
            return "";
        }
        return contenu.length() <= 120 ? contenu : contenu.substring(0, 120) + "…";
    }

    private ResponseEntity<?> sujetNonAccessible() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Ce sujet ne fait pas partie de vos cours."));
    }

    private boolean estVide(String valeur) {
        return valeur == null || valeur.trim().isEmpty();
    }
}
