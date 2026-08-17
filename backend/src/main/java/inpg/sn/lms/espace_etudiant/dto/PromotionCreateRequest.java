package inpg.sn.lms.espace_etudiant.dto;

import java.time.LocalDate;

/**
 * Création ou modification d'une promotion par le responsable pédagogique.
 *
 * <p>{@code formationId} n'est pas anodin : une promotion tire son programme de la
 * formation qu'elle suit. Laissée nulle, la promotion n'ouvre l'accès à aucun cours.
 */
public record PromotionCreateRequest(
        String code,
        String libelle,
        Long formationId,
        LocalDate dateDebut,
        LocalDate dateFin,
        String programme,
        String activitesParascolaires) {
}
