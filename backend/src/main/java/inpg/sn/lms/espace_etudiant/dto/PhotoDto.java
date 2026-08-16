package inpg.sn.lms.espace_etudiant.dto;

import java.time.LocalDateTime;

/** Cliché d'un album. {@code url} pointe vers l'endpoint qui sert le fichier. */
public record PhotoDto(
        Long id,
        String url,
        String legende,
        LocalDateTime dateAjout) {
}
