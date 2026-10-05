package inpg.sn.lms.espace_etudiant.dto;

import java.time.LocalDateTime;

/**
 * Cliché d'un album. {@code url} pointe vers l'endpoint qui sert le fichier ;
 * {@code nomOriginal} sert de nom de fichier au téléchargement depuis la
 * visionneuse (sans lui, le navigateur enregistre un fichier sans extension).
 */
public record PhotoDto(
        Long id,
        String url,
        String legende,
        String nomOriginal,
        LocalDateTime dateAjout) {
}
