package inpg.sn.lms.espace_etudiant.dto;

/**
 * Étudiant tel que le voient les écrans de suivi du formateur et du responsable
 * pédagogique.
 *
 * @param noteMoyenne moyenne pondérée par les coefficients, {@code null} tant qu'aucune
 *                    note n'a été saisie — à ne pas confondre avec un zéro. Ce champ
 *                    était un nombre simple, incapable de porter cette distinction :
 *                    un étudiant non évalué et un étudiant ayant tout raté s'affichaient
 *                    tous deux à 0,0, sur l'écran même où l'on décide qui relancer.
 */
public record EtudiantSuiviDto(
        Long id,
        String nom,
        String cohorte,
        String derniereConnexion,
        int progression,
        Double noteMoyenne,
        String statut,
        String initiales,
        String couleurAvatar,
        String matricule) {
}
