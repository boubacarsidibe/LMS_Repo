package inpg.sn.lms.recrutementformateur.dto;

/**
 * Décision du responsable admission sur une candidature de formateur.
 * {@code decision} vaut {@code RETENU} ou {@code NON_RETENU} ; {@code motif}
 * est obligatoire pour {@code NON_RETENU}, facultatif pour {@code RETENU}.
 */
public record DecisionCandidatureFormateurRequest(String decision, String motif) {
}
