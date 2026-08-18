package inpg.sn.lms.espace_etudiant.dto;

/**
 * Personne citable dans un fil du forum.
 *
 * <p>Ne porte ni adresse ni téléphone : cette liste alimente une autocomplétion
 * ouverte à tous les participants d'un fil, elle ne doit donc rien exposer de plus
 * qu'un nom et une qualité.
 *
 * @param role « Étudiant » ou « Formateur » — sert à distinguer deux homonymes, et à
 *             savoir à qui l'on s'adresse avant de cliquer
 */
public record MentionnableDto(
        Long id,
        String nom,
        String role) {
}
