package inpg.sn.lms.entreprise.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Fiche Technique de Module telle que servie à la pédagogie (tous statuts) et à
 * l'entreprise (jamais un {@code BROUILLON}). Reprend les 10 sections du
 * gabarit officiel de l'INPG.
 */
public record FicheTechniqueModuleDto(
        Long id,
        Long demandeId,
        String statut,
        LocalDateTime dateTransmission,
        String commentaireEntreprise,
        LocalDateTime dateDecisionEntreprise,

        String code,
        String intitule,
        String version,
        LocalDate dateRevision,
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

        List<FicheModuleJourDto> jours) {
}
