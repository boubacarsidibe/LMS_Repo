package inpg.sn.lms.accreditation.dto;

/**
 * Dépôt / mise à jour du jugement de l'évaluateur courant sur un critère.
 * {@code appreciation} : CONFORME, NON_CONFORME ou SANS_OBJET.
 */
public record EvaluationCritereRequest(String appreciation, String commentaire) {
}
