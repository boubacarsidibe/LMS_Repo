package inpg.sn.lms.accreditation.dto;

/**
 * Soumission d'une demande d'accréditation.
 *
 * <p>Porte les coordonnées de l'école <em>et</em> la description du programme
 * visé : le Cadre de référence DRCQ §II précise que l'accréditation concerne un
 * programme (avec sa cohorte et son type de structure), pas l'établissement.
 * {@code intituleProgramme}, {@code niveauProgramme} et {@code typeStructure}
 * sont obligatoires pour toute nouvelle demande (validés dans le contrôleur).
 */
public record DemandeAccreditationCreateRequest(
        String nomEcole,
        String adresse,
        String telephone,
        String siteWeb,
        String intituleProgramme,
        String niveauProgramme,
        String typeStructure,
        Integer effectifCohorte,
        String dateDebutCohorte) {
}
