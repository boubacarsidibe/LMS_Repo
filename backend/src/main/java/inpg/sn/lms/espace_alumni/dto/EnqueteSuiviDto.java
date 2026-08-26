package inpg.sn.lms.espace_alumni.dto;

import java.time.LocalDateTime;

/** Campagne d'enquête de suivi, vue côté alumni : indique s'il y a déjà répondu. */
public record EnqueteSuiviDto(
        Long id,
        String titre,
        String description,
        LocalDateTime dateCreation,
        boolean active,
        boolean dejaRepondu) {
}
