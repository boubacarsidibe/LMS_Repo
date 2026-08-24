package inpg.sn.lms.accreditation.dto;

/** Soumission d'une demande d'accréditation, avec les coordonnées de l'école. */
public record DemandeAccreditationCreateRequest(
        String nomEcole,
        String adresse,
        String telephone,
        String siteWeb) {
}
