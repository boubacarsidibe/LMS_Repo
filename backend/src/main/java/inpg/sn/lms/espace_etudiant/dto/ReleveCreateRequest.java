package inpg.sn.lms.espace_etudiant.dto;

import java.util.List;

/**
 * Demande de délivrance d'un ou plusieurs relevés.
 *
 * <p>La liste d'étudiants est plurielle à dessein : un relevé se délivre à une
 * promotion entière en fin de semestre, et le faire un par un serait le genre de
 * corvée qui pousse à ne pas le faire du tout.
 *
 * @param etudiantIds étudiants visés ; si vide, toute la promotion {@code promotionId}
 * @param periode     ce que le relevé couvre — « Semestre 1 — 2025/2026 »
 */
public record ReleveCreateRequest(
        List<Long> etudiantIds,
        Long promotionId,
        String periode) {
}
