package inpg.sn.lms.espace_etudiant.dto;

import java.time.LocalDateTime;

/** Évaluation de satisfaction déposée par l'étudiant sur un cours. */
public record EvaluationCoursDto(
        Long id,
        Long coursId,
        String coursTitre,
        int note,
        String commentaire,
        LocalDateTime dateSoumission) {
}
