package inpg.sn.lms.espace_etudiant.dto;

/**
 * Identité minimale d'un formateur, telle qu'affichée en regard d'un cours.
 *
 * <p>Ne porte ni téléphone ni adresse : ce DTO circule vers l'étudiant, à qui ces
 * coordonnées n'ont pas à être exposées.
 */
public record FormateurBrefDto(
        Long id,
        String nomComplet) {
}
