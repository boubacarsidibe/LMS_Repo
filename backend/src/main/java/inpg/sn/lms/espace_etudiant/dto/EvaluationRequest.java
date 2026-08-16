package inpg.sn.lms.espace_etudiant.dto;

/** Dépôt ou mise à jour d'une évaluation : une note de 1 à 5 et un commentaire libre. */
public record EvaluationRequest(Integer note, String commentaire) {
}
