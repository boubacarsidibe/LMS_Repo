package inpg.sn.lms.accreditation.dto;

/**
 * Création d'un critère d'accréditation via le CRUD de l'espace évaluateur.
 * {@code code} et {@code section} sont facultatifs : ils rattachent le critère à
 * une section du référentiel DRCQ ; un critère ad hoc peut s'en passer.
 */
public record CritereAccreditationRequest(String code, String section, String libelle, String description) {
}
