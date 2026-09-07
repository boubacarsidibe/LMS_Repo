package inpg.sn.lms.accreditation.dto;

import java.time.LocalDateTime;

/** Jugement de l'évaluateur connecté sur un critère du référentiel, pour un dossier. */
public record EvaluationCritereDto(
        Long critereId,
        String critereLibelle,
        String critereDescription,
        String appreciation,
        String commentaire,
        LocalDateTime dateEvaluation) {
}
