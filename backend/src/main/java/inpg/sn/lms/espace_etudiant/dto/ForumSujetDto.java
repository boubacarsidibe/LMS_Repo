package inpg.sn.lms.espace_etudiant.dto;

import java.time.LocalDateTime;

/**
 * Sujet du forum, résumé pour l'affichage en liste.
 *
 * <p>{@code nbNonLus} : messages d'un autre arrivés depuis la dernière ouverture du
 * fil par le connecté (pastille façon WhatsApp). {@code 0} là où la notion n'a pas
 * cours (forum alumni).
 */
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
        LocalDateTime dateDernierMessage,
        long nbNonLus) {

    /** Forme sans suivi de lecture, pour les écrans qui n'en ont pas. */
    public ForumSujetDto(Long id, String titre, Long coursId, String coursTitre, Long promotionId,
            String promotionLibelle, String auteur, LocalDateTime dateCreation, boolean epingle, boolean clos,
            long nbMessages, LocalDateTime dateDernierMessage) {
        this(id, titre, coursId, coursTitre, promotionId, promotionLibelle, auteur, dateCreation, epingle, clos,
                nbMessages, dateDernierMessage, 0);
    }
}
