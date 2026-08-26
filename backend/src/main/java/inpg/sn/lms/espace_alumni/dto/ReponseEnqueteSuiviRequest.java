package inpg.sn.lms.espace_alumni.dto;

/** Dépôt ou mise à jour d'une réponse : deux notes de 1 à 5 et un commentaire libre. */
public record ReponseEnqueteSuiviRequest(
        Integer noteApplicationCompetences,
        Integer noteImpactCarriere,
        String commentaire) {
}
