package inpg.sn.lms.espace_etudiant.dto;

import java.util.List;

/**
 * Relevé de notes d'un étudiant, groupé par cours.
 *
 * <p>Ces notes existaient en base depuis l'origine mais n'étaient lues que par deux
 * endpoints, tous deux réservés au formateur : l'étudiant n'avait aucun moyen de
 * consulter ses propres résultats. Le champ {@code maNote} de
 * {@link CoursEtudiantDto} ne s'y substituait pas — il porte l'appréciation que
 * l'étudiant donne au cours, pas la note qu'il reçoit.
 *
 * @param moyenneGenerale moyenne pondérée par les coefficients des cours, {@code null}
 *                        s'il n'y a aucune note
 * @param nombreNotes     total, pour distinguer « pas encore de note » de « moyenne de 0 »
 */
public record ReleveNotesDto(
        Double moyenneGenerale,
        int nombreNotes,
        List<NotesCoursDto> parCours) {

    /**
     * Notes obtenues dans un cours.
     *
     * @param moyenne     moyenne du cours, {@code null} si aucune note n'y a été saisie
     * @param coefficient poids de ce cours dans la moyenne générale ; affiché à l'étudiant
     *                    pour que son relevé soit vérifiable
     */
    public record NotesCoursDto(
            Long coursId,
            String coursCode,
            String coursTitre,
            Double moyenne,
            double coefficient,
            List<NoteModuleDto> notes) {
    }
}
