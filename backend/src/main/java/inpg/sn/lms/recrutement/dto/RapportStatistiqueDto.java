package inpg.sn.lms.recrutement.dto;

import java.util.List;

/**
 * Rapport statistique du recrutement, en PDF (demande responsable admission).
 * Recalcule exactement les mêmes agrégats que la page Statistiques du
 * portail admission ({@code responsable-admission-statistiques.component.ts})
 * — même définitions, mêmes arrondis — pour que le PDF téléchargé corresponde
 * toujours à ce que l'écran affiche au même instant.
 */
public record RapportStatistiqueDto(
        int totalCandidats,
        int validationEnCours,
        int preselectionnes,
        int rejetes,
        List<Segment> repartitionStatuts,
        int tauxAdmission,
        int candidaturesDecidees,
        List<Segment> repartitionGenre,
        List<Segment> repartitionAge,
        List<Segment> topProfessions,
        int nbSansProfessionDeclaree,
        List<Segment> topNationalites) {

    public record Segment(String label, int count, int pourcentage) {
    }
}
