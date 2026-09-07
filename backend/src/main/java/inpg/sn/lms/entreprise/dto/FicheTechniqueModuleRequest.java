package inpg.sn.lms.entreprise.dto;

import java.util.List;

/**
 * Corps d'un enregistrement (upsert) de Fiche Technique de Module par le
 * responsable pédagogique. Le statut n'est pas piloté ici : il évolue via les
 * endpoints dédiés (transmettre / accepter / ajustements).
 */
public record FicheTechniqueModuleRequest(
        String code,
        String intitule,
        String version,
        String dateRevision,
        String responsableModule,

        String departement,
        String atelierService,
        String fonctionsCibles,
        String thematiques,
        String prerequis,
        String langue,
        String niveau,
        Integer effectif,
        Integer dureeHeures,
        Integer dureeJours,
        String periodeProbable,

        String objectifGeneral,
        String objectifsSpecifiques,
        String modalitesTestPrerequis,
        String formationSurSite,
        String typesEvaluation,
        String ressourcesPedagogiques,
        String bibliographie,
        String profilFormateur,

        String coutTotal,
        String modalitesFinancieres,

        List<JourRequest> jours) {

    /** Une ligne du planning jour par jour. {@code id} ignoré (la collection est recréée). */
    public record JourRequest(
            int numero,
            String contenus,
            Integer dureeHeures,
            String typeEnseignement,
            String modalites) {
    }
}
