package inpg.sn.lms.espace_etudiant.dto;

import java.time.LocalDateTime;

/**
 * Message du forum. {@code estAuteur} permet au client de distinguer les messages
 * du lecteur sans lui exposer d'identifiants d'utilisateur.
 *
 * <p>{@code reponseA*} porte un résumé du message cité (façon WhatsApp), null quand
 * le message ne répond à rien de précis — le client n'a pas besoin d'aller
 * rechercher le message d'origine pour l'afficher au-dessus de la réponse.
 */
public record ForumMessageDto(
        Long id,
        String auteur,
        String contenu,
        LocalDateTime dateCreation,
        boolean estAuteur,
        Long reponseAId,
        String reponseAAuteur,
        String reponseAExtrait) {
}
