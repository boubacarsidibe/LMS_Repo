package inpg.sn.lms.espace_etudiant.controller;

import java.io.IOException;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import inpg.sn.lms.espace_etudiant.entity.Cours;
import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.auth.entity.Formateur;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.EtudiantRepository;
import inpg.sn.lms.auth.repository.FormateurRepository;
import inpg.sn.lms.espace_etudiant.dto.ConversationCreateRequest;
import inpg.sn.lms.espace_etudiant.dto.ConversationDto;
import inpg.sn.lms.espace_etudiant.dto.MentionnableDto;
import inpg.sn.lms.espace_etudiant.dto.MessagePriveDto;
import inpg.sn.lms.espace_etudiant.entity.Conversation;
import inpg.sn.lms.espace_etudiant.entity.MessagePrive;
import inpg.sn.lms.espace_etudiant.entity.Notification;
import inpg.sn.lms.espace_etudiant.repository.ConversationRepository;
import inpg.sn.lms.espace_etudiant.repository.MessagePriveRepository;
import inpg.sn.lms.espace_etudiant.repository.NotificationRepository;
import inpg.sn.lms.espace_etudiant.service.HabilitationCoursService;
import inpg.sn.lms.espace_etudiant.service.PerimetreFormateurService;
import inpg.sn.lms.espace_etudiant.service.UtilisateurCourantService;
import inpg.sn.lms.fichier.PieceJointeStorage;

/**
 * Messagerie privée apprenant ↔ formateur. Distincte du forum : un fil ici
 * n'est visible que de ses deux participants, sans portée cours ou
 * promotion — ce n'est pas un canal de groupe qui se trouve n'avoir que deux
 * membres, c'est un modèle de visibilité différent.
 *
 * <p>Portée à ÉTUDIANT/FORMATEUR seulement, à dessein : l'espace alumni vit
 * sous son propre préfixe {@code /api/espace-alumni/**}, verrouillé côté
 * {@code SecurityConfig} au rôle ALUMNI exclusivement. Ajouter ALUMNI ici
 * sans toucher SecurityConfig reproduirait le piège déjà rencontré ailleurs
 * dans ce dépôt : une autorisation posée au niveau méthode mais jamais
 * atteinte, parce que la couche URL, évaluée avant, a déjà renvoyé 403.
 */
@RestController
@RequestMapping("/api/espace-etudiant/messagerie")
@PreAuthorize("hasAnyRole('ETUDIANT','FORMATEUR')")
public class MessagerieController {

    private static final DateTimeFormatter JOUR_HEURE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ConversationRepository conversationRepository;
    private final MessagePriveRepository messageRepository;
    private final EtudiantRepository etudiantRepository;
    private final FormateurRepository formateurRepository;
    private final HabilitationCoursService habilitation;
    private final PerimetreFormateurService perimetreFormateur;
    private final UtilisateurCourantService utilisateurCourant;
    private final NotificationRepository notificationRepository;
    private final Path uploadDirectory;

    public MessagerieController(ConversationRepository conversationRepository,
            MessagePriveRepository messageRepository, EtudiantRepository etudiantRepository,
            FormateurRepository formateurRepository, HabilitationCoursService habilitation,
            PerimetreFormateurService perimetreFormateur, UtilisateurCourantService utilisateurCourant,
            NotificationRepository notificationRepository,
            @Value("${app.upload.dir:uploads}") String uploadDirectory) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.etudiantRepository = etudiantRepository;
        this.formateurRepository = formateurRepository;
        this.habilitation = habilitation;
        this.perimetreFormateur = perimetreFormateur;
        this.utilisateurCourant = utilisateurCourant;
        this.notificationRepository = notificationRepository;
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

    /**
     * Personnes qu'on peut contacter : un étudiant voit les formateurs de ses
     * cours, un formateur voit les étudiants de ses cohortes. Même principe
     * que les citations du forum — la liste suit l'accès, elle ne l'ouvre pas.
     */
    @GetMapping("/contactables")
    @Transactional(readOnly = true)
    public ResponseEntity<?> contactables(Authentication authentication) {
        User utilisateur = utilisateurCourant.utilisateur(authentication);
        if (utilisateur == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Map<Long, MentionnableDto> parId = new LinkedHashMap<>();
        Etudiant etudiant = utilisateurCourant.etudiant(authentication);
        if (etudiant != null) {
            for (Cours cours : habilitation.coursAutorises(etudiant)) {
                for (Formateur formateur : cours.getFormateurs()) {
                    if (formateur.getUser() != null) {
                        parId.putIfAbsent(formateur.getUser().getId(), new MentionnableDto(
                                formateur.getUser().getId(),
                                (formateur.getPrenom() + " " + formateur.getNom()).trim(),
                                "Formateur"));
                    }
                }
            }
        } else {
            Formateur formateur = formateurRepository.findByUserId(utilisateur.getId()).orElse(null);
            if (formateur != null) {
                List<Long> cohorteIds = perimetreFormateur.cohorteIdsDuFormateur(formateur);
                for (Etudiant e : etudiantRepository.findByPromotion_IdIn(cohorteIds)) {
                    if (e.getUser() != null) {
                        parId.putIfAbsent(e.getUser().getId(), new MentionnableDto(
                                e.getUser().getId(), (e.getPrenom() + " " + e.getNom()).trim(), "Étudiant"));
                    }
                }
            }
        }
        return ResponseEntity.ok(parId.values());
    }

    @GetMapping("/conversations")
    @Transactional(readOnly = true)
    public ResponseEntity<?> mesConversations(Authentication authentication) {
        User utilisateur = utilisateurCourant.utilisateur(authentication);
        if (utilisateur == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<ConversationDto> conversations = conversationRepository.findDeParticipant(utilisateur.getId()).stream()
                .map(c -> toConversationDto(c, utilisateur))
                .sorted((a, b) -> b.dateDernierMessage().compareTo(a.dateDernierMessage()))
                .toList();
        return ResponseEntity.ok(conversations);
    }

    /** Ouvre une conversation si elle n'existe pas déjà, et y dépose le premier message. */
    @PostMapping("/conversations")
    @Transactional
    public ResponseEntity<?> demarrerConversation(@RequestBody ConversationCreateRequest request,
            Authentication authentication) {
        User utilisateur = utilisateurCourant.utilisateur(authentication);
        if (utilisateur == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (request.destinataireId() == null || estVide(request.contenu())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le destinataire et le message sont requis."));
        }
        if (!estContactable(utilisateur, request.destinataireId(), authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Cette personne ne fait pas partie de vos contacts."));
        }
        Conversation conversation = conversationRepository
                .findEntreParticipants(utilisateur.getId(), request.destinataireId())
                .orElseGet(() -> {
                    User destinataire = etudiantRepository.findById(request.destinataireId())
                            .map(Etudiant::getUser)
                            .orElseGet(() -> formateurRepository.findById(request.destinataireId())
                                    .map(Formateur::getUser).orElse(null));
                    return conversationRepository.save(new Conversation(utilisateur, destinataire));
                });
        MessagePrive message = messageRepository.save(new MessagePrive(conversation, utilisateur, request.contenu().trim()));
        notifierNouveauMessage(conversation, message, utilisateur);
        return ResponseEntity.status(HttpStatus.CREATED).body(toConversationDto(conversation, utilisateur));
    }

    /** Fil complet, et marque comme lus les messages reçus. */
    @GetMapping("/conversations/{id}")
    @Transactional
    public ResponseEntity<?> fil(@PathVariable Long id, Authentication authentication) {
        User utilisateur = utilisateurCourant.utilisateur(authentication);
        if (utilisateur == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Conversation conversation = conversationRepository.findById(id).orElse(null);
        if (conversation == null || !estParticipant(conversation, utilisateur)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Cette conversation ne vous est pas accessible."));
        }
        messageRepository.findByConversationIdAndAuteurIdNotAndLuFalse(id, utilisateur.getId())
                .forEach(m -> m.setLu(true));
        List<MessagePriveDto> messages = messageRepository.findByConversationIdOrderByDateEnvoiAsc(id).stream()
                .map(m -> toMessageDto(m, utilisateur))
                .toList();
        return ResponseEntity.ok(Map.of(
                "conversation", toConversationDto(conversation, utilisateur),
                "messages", messages));
    }

    @PostMapping(value = "/conversations/{id}/messages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<?> repondre(@PathVariable Long id,
            @RequestParam String contenu,
            @RequestParam(value = "fichier", required = false) MultipartFile fichier,
            Authentication authentication) {
        User utilisateur = utilisateurCourant.utilisateur(authentication);
        if (utilisateur == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Conversation conversation = conversationRepository.findById(id).orElse(null);
        if (conversation == null || !estParticipant(conversation, utilisateur)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Cette conversation ne vous est pas accessible."));
        }
        if (estVide(contenu)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le message ne peut pas être vide."));
        }

        MessagePrive message = new MessagePrive(conversation, utilisateur, contenu.trim());
        if (fichier != null && !fichier.isEmpty()) {
            try {
                PieceJointeStorage.Enregistree stockee = PieceJointeStorage.enregistrer(fichier, uploadDirectory, "messages");
                message.setPieceJointeNomOriginal(stockee.nomOriginal());
                message.setPieceJointeNomStocke(stockee.nomStocke());
                message.setPieceJointeTypeContenu(stockee.typeContenu());
            } catch (IllegalArgumentException invalide) {
                return ResponseEntity.badRequest().body(Map.of("message", invalide.getMessage()));
            } catch (IOException exception) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Le fichier n'a pas pu être enregistré."));
            }
        }
        message = messageRepository.save(message);
        notifierNouveauMessage(conversation, message, utilisateur);
        return ResponseEntity.status(HttpStatus.CREATED).body(toMessageDto(message, utilisateur));
    }

    /**
     * Le message porte à sa conversation, dont seuls les deux participants ont le
     * droit de lire les pièces jointes — même recoupement que {@code fil}/{@code
     * repondre} ; sans lui, l'identifiant de message suffirait à lire un document
     * échangé entre deux tiers.
     */
    @GetMapping("/messages/{messageId}/piece-jointe")
    @Transactional(readOnly = true)
    public ResponseEntity<Resource> telechargerPieceJointe(@PathVariable Long messageId, Authentication authentication) {
        User utilisateur = utilisateurCourant.utilisateur(authentication);
        MessagePrive message = messageRepository.findById(messageId).orElse(null);
        if (utilisateur == null || message == null || message.getPieceJointeNomStocke() == null
                || !estParticipant(message.getConversation(), utilisateur)) {
            return ResponseEntity.notFound().build();
        }
        return PieceJointeStorage.servir(uploadDirectory, "messages",
                message.getPieceJointeNomStocke(), message.getPieceJointeNomOriginal(),
                message.getPieceJointeTypeContenu());
    }

    // ===================== Habilitation =====================

    private boolean estParticipant(Conversation conversation, User utilisateur) {
        return conversation.getParticipant1().getId().equals(utilisateur.getId())
                || conversation.getParticipant2().getId().equals(utilisateur.getId());
    }

    /** Recoupe le destinataire demandé avec la liste réellement contactable, comme les citations du forum. */
    private boolean estContactable(User utilisateur, Long destinataireId, Authentication authentication) {
        Etudiant etudiant = utilisateurCourant.etudiant(authentication);
        if (etudiant != null) {
            return habilitation.coursAutorises(etudiant).stream()
                    .flatMap(c -> c.getFormateurs().stream())
                    .anyMatch(f -> f.getUser() != null && f.getUser().getId().equals(destinataireId));
        }
        Formateur formateur = formateurRepository.findByUserId(utilisateur.getId()).orElse(null);
        if (formateur == null) {
            return false;
        }
        List<Long> cohorteIds = perimetreFormateur.cohorteIdsDuFormateur(formateur);
        return etudiantRepository.findByPromotion_IdIn(cohorteIds).stream()
                .anyMatch(e -> e.getUser() != null && e.getUser().getId().equals(destinataireId));
    }

    private void notifierNouveauMessage(Conversation conversation, MessagePrive message, User auteur) {
        User destinataire = conversation.autreParticipant(auteur);
        notificationRepository.save(new Notification(
                destinataire,
                utilisateurCourant.nomAffiche(auteur) + " vous a envoyé un message.",
                Notification.MESSAGE_PRIVE,
                lienDeLaConversation(conversation.getId(), destinataire)));
    }

    /**
     * L'espace formateur n'a pas la même racine de route que l'espace étudiant
     * (même piège déjà rencontré pour les réponses du forum) : sans cette
     * distinction, une notification adressée à un formateur pointait vers une
     * URL que son espace ne sait pas résoudre.
     */
    private String lienDeLaConversation(Long conversationId, User destinataire) {
        boolean estFormateur = formateurRepository.findByUserId(destinataire.getId()).isPresent();
        return (estFormateur ? "/espace-etudiant/formateur/messagerie/" : "/espace-etudiant/messagerie/") + conversationId;
    }

    // ===================== Correspondances =====================

    private ConversationDto toConversationDto(Conversation conversation, User utilisateur) {
        User autre = conversation.autreParticipant(utilisateur);
        MessagePrive dernier = messageRepository.findFirstByConversationIdOrderByDateEnvoiDesc(conversation.getId());
        long nonLus = messageRepository.countByConversationIdAndAuteurIdNotAndLuFalse(conversation.getId(), utilisateur.getId());
        return new ConversationDto(
                conversation.getId(),
                autre == null ? null : autre.getId(),
                autre == null ? "Utilisateur supprimé" : utilisateurCourant.nomAffiche(autre),
                dernier == null ? null : dernier.getContenu(),
                dernier == null ? conversation.getDateCreation().format(JOUR_HEURE) : dernier.getDateEnvoi().format(JOUR_HEURE),
                nonLus);
    }

    private MessagePriveDto toMessageDto(MessagePrive message, User lecteur) {
        return new MessagePriveDto(
                message.getId(),
                utilisateurCourant.nomAffiche(message.getAuteur()),
                message.getAuteur().getId().equals(lecteur.getId()),
                message.getContenu(),
                message.getDateEnvoi().format(JOUR_HEURE),
                message.getPieceJointeNomOriginal());
    }

    private boolean estVide(String valeur) {
        return valeur == null || valeur.trim().isEmpty();
    }
}
