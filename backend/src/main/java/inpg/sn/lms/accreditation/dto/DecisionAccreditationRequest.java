package inpg.sn.lms.accreditation.dto;

/** Décision d'un évaluateur. {@code motif} est ignoré pour une accréditation, obligatoire pour un rejet. */
public record DecisionAccreditationRequest(String motif) {
}
