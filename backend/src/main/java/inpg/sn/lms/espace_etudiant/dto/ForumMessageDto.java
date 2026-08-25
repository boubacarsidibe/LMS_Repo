package inpg.sn.lms.espace_etudiant.dto;

import java.time.LocalDateTime;

/**
 * Message du forum. {@code estAuteur} permet au client de distinguer les messages
 * du lecteur sans lui exposer d'identifiants d'utilisateur.
 */
public record ForumMessageDto(
        Long id,
        String auteur,
        String contenu,
        LocalDateTime dateCreation,
        boolean estAuteur) {
}
