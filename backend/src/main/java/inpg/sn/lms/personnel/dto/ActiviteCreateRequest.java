package inpg.sn.lms.personnel.dto;

/** {@code typeActivite} : FORMATION, PROJET, RECHERCHE ou AUTRE. */
public record ActiviteCreateRequest(
        Long departementId,
        String typeActivite,
        String titre,
        String description,
        Integer mois,
        Integer annee) {
}
