package inpg.sn.lms.espace_etudiant.dto;

import java.time.LocalDate;

/**
 * Création d'un album. {@code promotionId} à {@code null} crée un album
 * institutionnel, visible de tous les étudiants.
 */
public record AlbumCreateRequest(
        String titre,
        String description,
        Long promotionId,
        LocalDate dateEvenement) {
}
