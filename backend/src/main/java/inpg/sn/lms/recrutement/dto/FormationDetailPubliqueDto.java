package inpg.sn.lms.recrutement.dto;

/**
 * Fiche détaillée d'une formation.
 *
 * <p>{@code ficheDetaillee} est transmise telle quelle : elle porte le contenu
 * éditorial (modules, méthode pédagogique, points clés, débouchés, encadré
 * latéral) dans la forme qu'attend déjà le gabarit de la page de détail.
 *
 * <p><strong>Ce champ est une structure neutre ({@code Map}/{@code List}), jamais un {@code JsonNode}.</strong>
 * Spring Boot 4 sérialise avec Jackson 3 ({@code tools.jackson}) ; le {@code JsonNode} de Jackson 2
 * ({@code com.fasterxml}), qu'utilise le reste du code, n'y est pas reconnu et sortait comme un objet Java
 * ordinaire ({@code "array": false, "containerNode": true…}) : les fiches de formation du site public arrivaient
 * vides au navigateur. Une Map est comprise de toutes les versions de Jackson.
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
        Object ficheDetaillee,
        Integer heuresSimulationLabo,
        boolean anglaisProfessionnel,
        boolean softSkills,
        Integer creditsTotaux,
        Integer volumeHoraireTotal) {
}
