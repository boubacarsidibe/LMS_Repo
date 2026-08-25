package inpg.sn.lms.espace_etudiant.dto;

/**
 * Profil d'un formateur.
 *
 * <p>{@code nom} reste le nom complet (« Prénom Nom ») utilisé pour l'affichage,
 * mais {@code prenom} / {@code nomFamille} / {@code telephone} sont exposés
 * séparément : sans eux, l'écran de modification devrait redécouper le nom complet
 * pour préremplir ses champs, ce qui échoue dès qu'un nom comporte un espace.
 */
public record FormateurProfileDto(
        Long id,
        String nom,
        String prenom,
        String nomFamille,
        String telephone,
        String email,
        long nbCours) {
}
