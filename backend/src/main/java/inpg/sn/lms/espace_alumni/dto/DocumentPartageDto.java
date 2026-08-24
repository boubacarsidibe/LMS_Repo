package inpg.sn.lms.espace_alumni.dto;

import java.time.LocalDateTime;

/** Document partagé, résumé pour l'affichage en liste. */
public record DocumentPartageDto(
        Long id,
        String titre,
        String description,
        String auteur,
        String nomFichierOriginal,
        LocalDateTime dateAjout,
        boolean estAuteur) {
}
