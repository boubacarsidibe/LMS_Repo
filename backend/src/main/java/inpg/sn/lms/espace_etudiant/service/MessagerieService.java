package inpg.sn.lms.espace_etudiant.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.FormateurRepository;
import inpg.sn.lms.espace_etudiant.entity.Conversation;
import inpg.sn.lms.espace_etudiant.entity.MessagePrive;
import inpg.sn.lms.espace_etudiant.entity.Notification;
import inpg.sn.lms.espace_etudiant.repository.ConversationRepository;
import inpg.sn.lms.espace_etudiant.repository.MessagePriveRepository;
import inpg.sn.lms.espace_etudiant.repository.NotificationRepository;

/**
 * Dépôt d'un message privé, extrait de {@code MessagerieController} pour être
 * réutilisable par un second appelant : la relance automatique d'absence
 * (voir {@code EspaceEtudiantController#relancerAbsences}) ouvre une
 * conversation exactement comme {@code démarrerConversation}, mais à
 * l'initiative du formateur plutôt que de l'utilisateur connecté qui clique —
 * dupliquer la logique de conversation (trouver-ou-créer, notifier, lien
 * selon le rôle du destinataire) aurait fait deux sources de vérité pour la
 * même règle.
 */
@Service
public class MessagerieService {

    private final ConversationRepository conversationRepository;
    private final MessagePriveRepository messageRepository;
    private final NotificationRepository notificationRepository;
    private final FormateurRepository formateurRepository;
    private final UtilisateurCourantService utilisateurCourant;

    public MessagerieService(ConversationRepository conversationRepository,
            MessagePriveRepository messageRepository, NotificationRepository notificationRepository,
            FormateurRepository formateurRepository, UtilisateurCourantService utilisateurCourant) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.notificationRepository = notificationRepository;
        this.formateurRepository = formateurRepository;
        this.utilisateurCourant = utilisateurCourant;
    }

    /** Ouvre la conversation entre les deux si elle n'existe pas déjà, y dépose le message et notifie. */
    @Transactional
    public MessagePrive envoyer(User auteur, User destinataire, String contenu) {
        Conversation conversation = conversationRepository
                .findEntreParticipants(auteur.getId(), destinataire.getId())
                .orElseGet(() -> conversationRepository.save(new Conversation(auteur, destinataire)));
        return deposer(conversation, auteur, contenu);
    }

    /**
     * Dépose un message dans une conversation déjà connue de l'appelant (celui-ci
     * a déjà l'identifiant de la conversation, pas seulement celui du
     * destinataire) : pas de recherche « trouver ou créer » ici, sans quoi
     * {@code MessagerieController#repondre} referait une recherche par
     * participants redondante avec le {@code conversationRepository.findById}
     * qu'il vient de faire.
     */
    @Transactional
    public MessagePrive deposerDans(Conversation conversation, User auteur, String contenu) {
        return deposer(conversation, auteur, contenu);
    }

    private MessagePrive deposer(Conversation conversation, User auteur, String contenu) {
        MessagePrive message = messageRepository.save(new MessagePrive(conversation, auteur, contenu));
        User destinataire = conversation.autreParticipant(auteur);
        notificationRepository.save(new Notification(
                destinataire,
                utilisateurCourant.nomAffiche(auteur) + " vous a envoyé un message.",
                Notification.MESSAGE_PRIVE,
                lienDeLaConversation(conversation.getId(), destinataire)));
        return message;
    }

    /**
     * L'espace formateur n'a pas la même racine de route que l'espace étudiant
     * (même piège qu'au forum) : sans cette distinction, une notification
     * adressée à un formateur pointerait vers une URL que son espace ne sait
     * pas résoudre.
     */
    private String lienDeLaConversation(Long conversationId, User destinataire) {
        boolean estFormateur = formateurRepository.findByUserId(destinataire.getId()).isPresent();
        return (estFormateur ? "/espace-etudiant/formateur/messagerie/" : "/espace-etudiant/messagerie/") + conversationId;
    }
}
