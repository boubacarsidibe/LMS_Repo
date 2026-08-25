package inpg.sn.lms.espace_etudiant.dto;

/**
 * Ouverture d'un sujet. {@code coursId} à {@code null} crée un sujet général,
 * visible de tout l'espace étudiant.
 */
public record ForumSujetCreateRequest(
        String titre,
        Long coursId,
        String premierMessage) {
}
