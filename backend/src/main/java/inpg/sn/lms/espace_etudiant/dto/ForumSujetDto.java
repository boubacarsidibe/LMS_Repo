package inpg.sn.lms.espace_etudiant.dto;

import java.time.LocalDateTime;

/** Sujet du forum, résumé pour l'affichage en liste. */
public record ForumSujetDto(
        Long id,
        String titre,
        Long coursId,
        String coursTitre,
        Long promotionId,
        String promotionLibelle,
        String auteur,
        LocalDateTime dateCreation,
        boolean epingle,
        boolean clos,
        long nbMessages,
        LocalDateTime dateDernierMessage) {
}
