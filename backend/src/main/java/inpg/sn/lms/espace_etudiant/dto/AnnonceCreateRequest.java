package inpg.sn.lms.espace_etudiant.dto;

import inpg.sn.lms.espace_etudiant.enums.EnumTypeAnnonce;

/**
 * Publication d'une annonce, d'un message ou d'un mémo.
 *
 * <p>{@code promotionId} à {@code null} vaut diffusion à tous les étudiants.
 */
public record AnnonceCreateRequest(
        String titre,
        String contenu,
        EnumTypeAnnonce type,
        Long promotionId,
        Boolean epinglee) {
}
