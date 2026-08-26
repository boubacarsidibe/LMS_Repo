package inpg.sn.lms.personnel.dto;

import java.time.LocalDateTime;

public record ActiviteDto(
        Long id,
        Long departementId,
        String departementLibelle,
        String auteurNom,
        String typeActivite,
        String titre,
        String description,
        int mois,
        int annee,
        LocalDateTime dateCreation) {
}
