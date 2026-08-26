package inpg.sn.lms.espace_etudiant.dto;

/**
 * Fiche complète d'un étudiant, telle que consultée par le responsable
 * pédagogique en ouvrant sa ligne dans le répertoire.
 *
 * <p>Distinct de {@link EtudiantSuiviDto}, qui alimente aussi l'écran du
 * formateur : l'email et le téléphone n'ont pas à y circuler, seul le
 * responsable pédagogique a besoin de coordonnées complètes.
 */
public record EtudiantDetailDto(
        Long id,
        String nom,
        String prenom,
        String email,
        String telephone,
        String username,
        String matricule,
        Long promotionId,
        String promotion,
        Double noteMoyenne,
        String statut,
        String origine,
        boolean estAlumni) {
}
