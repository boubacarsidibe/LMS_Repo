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
        String matricule,
        /**
         * D'où vient ce compte : {@code "Admission"} s'il a été ouvert par la
         * conversion d'un candidat admis, {@code null} s'il a été créé à la main.
         *
         * <p>Sans cette distinction, un candidat fraîchement admis et un compte que le
         * responsable pédagogique avait oublié d'affecter s'affichent à l'identique :
         * « sans promotion ». Le premier appelle une inscription, le second une
         * correction — ce ne sont pas les mêmes gestes.
         */
        String origine,
        /**
         * Vrai si ce compte est déjà passé côté alumni. La ligne {@code Etudiant} n'est
         * pas supprimée à la conversion (voir migration V12), elle continue donc
         * d'apparaître dans ce répertoire — sans ce marqueur, rien ne distinguerait un
         * étudiant actif d'un ancien étudiant, et l'action de promotion resterait
         * proposée indéfiniment sur un compte déjà converti.
         */
        boolean estAlumni) {
}
