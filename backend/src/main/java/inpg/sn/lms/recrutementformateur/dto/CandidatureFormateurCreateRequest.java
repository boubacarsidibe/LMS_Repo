package inpg.sn.lms.recrutementformateur.dto;

/**
 * Dépôt d'une candidature à une offre de recrutement de formateur.
 * {@code offreId}, {@code nom}, {@code prenom} et {@code telephone} sont
 * obligatoires (validés dans le contrôleur) ; l'e-mail est repris du compte
 * connecté.
 */
public record CandidatureFormateurCreateRequest(
        Long offreId,
        String nom,
        String prenom,
        String telephone,
        String diplomePrincipal,
        Integer anneesExperience,
        String lettreMotivation) {
}
