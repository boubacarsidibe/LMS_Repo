package inpg.sn.lms.espace_etudiant.dto;

/**
 * Synthèse qualité d'un cours (TDR §3.7) : moyenne et volume des retours.
 * {@code moyenne} vaut {@code null} tant qu'aucune évaluation n'a été déposée.
 */
public record SyntheseEvaluationDto(
        Long coursId,
        String coursTitre,
        Double moyenne,
        long nombreEvaluations) {
}
