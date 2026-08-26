package inpg.sn.lms.espace_etudiant.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Synthèse d'une campagne d'enquête de suivi, vue pédagogie (TDR §G, volet
 * « impact carrière » du tableau de bord qualité).
 *
 * @param moyenneApplicationCompetences moyenne sur 5, {@code null} si aucune réponse
 * @param moyenneImpactCarriere         moyenne sur 5, {@code null} si aucune réponse
 */
public record SyntheseEnqueteSuiviDto(
        Long enqueteId,
        String titre,
        String description,
        LocalDateTime dateCreation,
        boolean active,
        int nombreReponses,
        Double moyenneApplicationCompetences,
        Double moyenneImpactCarriere,
        List<String> derniersCommentaires) {
}
