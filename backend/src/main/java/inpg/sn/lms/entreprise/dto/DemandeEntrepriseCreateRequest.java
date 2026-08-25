package inpg.sn.lms.entreprise.dto;

/**
 * Soumission d'une demande (besoin de formation ou devis/session sur mesure).
 * {@code formationId}, {@code effectifConcerne} et {@code datesSouhaitees} ne
 * sont pertinents que pour un {@code type = "DEVIS_SESSION"} — laissés
 * {@code null} pour un simple besoin.
 */
public record DemandeEntrepriseCreateRequest(
        EntrepriseProfilRequest entreprise,
        String type,
        Long formationId,
        String description,
        Integer effectifConcerne,
        String datesSouhaitees) {
}
