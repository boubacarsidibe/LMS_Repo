package inpg.sn.lms.espace_etudiant.dto;

import java.time.LocalDate;

/**
 * Fiche de promotion présentée à l'étudiant (TDR §3.2 : programme, calendrier,
 * activités parascolaires, cours et formateurs).
 */
public record PromotionDetailDto(
        Long id,
        String code,
        String libelle,
        LocalDate dateDebut,
        LocalDate dateFin,
        boolean enCours,
        String programme,
        String activitesParascolaires,
        String formationCode,
        String formationIntitule,
        String formationDescription,
        long effectif,
        long nbCours) {
}
