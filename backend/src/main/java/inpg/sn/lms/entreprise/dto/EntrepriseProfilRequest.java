package inpg.sn.lms.entreprise.dto;

/** Coordonnées de l'entreprise, saisies (ou mises à jour) à chaque soumission. */
public record EntrepriseProfilRequest(
        String raisonSociale,
        String secteur,
        String adresse,
        String telephone) {
}
