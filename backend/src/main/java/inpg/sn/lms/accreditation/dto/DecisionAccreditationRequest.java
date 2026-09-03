package inpg.sn.lms.accreditation.dto;

/**
 * Décision d'un évaluateur sur un dossier d'accréditation.
 *
 * <ul>
 *   <li>{@code motif} — ignoré pour une accréditation pleine, obligatoire pour un rejet
 *       (devient la synthèse transmise à l'école).</li>
 *   <li>{@code planActions} — obligatoire pour une accréditation conditionnelle
 *       (« sous réserve »), sinon ignoré.</li>
 * </ul>
 */
public record DecisionAccreditationRequest(String motif, String planActions) {
}
