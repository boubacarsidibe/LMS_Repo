package inpg.sn.lms.recrutementformateur.dto;

/**
 * Création / mise à jour d'une offre de recrutement de formateur.
 * {@code intitule} et {@code typeContrat} sont obligatoires (validés dans le
 * contrôleur). Le statut n'est pas piloté ici : il évolue via
 * {@code publier} / {@code cloturer}.
 */
public record OffreFormateurRequest(
        String intitule,
        String domaine,
        String descriptionMissions,
        String profilRecherche,
        String typeContrat,
        String lieu,
        Integer nbPostes,
        Long formationId,
        String dateLimiteCandidature) {
}
