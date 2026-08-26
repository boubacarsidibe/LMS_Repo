package inpg.sn.lms.espace_alumni.dto;

import java.time.LocalDateTime;

public record ReponseEnqueteSuiviDto(
        Long id,
        Long enqueteId,
        int noteApplicationCompetences,
        int noteImpactCarriere,
        String commentaire,
        LocalDateTime dateReponse) {
}
