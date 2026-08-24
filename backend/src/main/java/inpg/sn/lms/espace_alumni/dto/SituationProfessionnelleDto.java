package inpg.sn.lms.espace_alumni.dto;

import java.time.LocalDate;

public record SituationProfessionnelleDto(
        Long id,
        String intitulePoste,
        String entreprise,
        LocalDate dateDebut,
        LocalDate dateFin) {
}
