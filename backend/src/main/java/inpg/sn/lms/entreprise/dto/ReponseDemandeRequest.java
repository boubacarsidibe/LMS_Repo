package inpg.sn.lms.entreprise.dto;

/** Réponse de l'équipe pédagogique à une demande, avec le nouveau statut à appliquer. */
public record ReponseDemandeRequest(
        String statut,
        String reponse) {
}
