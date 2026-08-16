package inpg.sn.lms.recrutement.dto;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Fiche détaillée d'une formation.
 *
 * <p>{@code ficheDetaillee} est transmise telle quelle : elle porte le contenu
 * éditorial (modules, méthode pédagogique, points clés, débouchés, encadré
 * latéral) dans la forme qu'attend déjà le gabarit de la page de détail.
 */
public record FormationDetailPubliqueDto(
        Long id,
        String code,
        String intitule,
        String description,
        String niveau,
        String domaine,
        String duree,
        String imageUrl,
        JsonNode ficheDetaillee) {
}
