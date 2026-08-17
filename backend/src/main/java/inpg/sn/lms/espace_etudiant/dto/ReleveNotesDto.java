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
 * @param moyenneGenerale moyenne de toutes les notes, {@code null} s'il n'y en a aucune
 * @param nombreNotes     total, pour distinguer « pas encore de note » de « moyenne de 0 »
 */
public record ReleveNotesDto(
        Double moyenneGenerale,
        int nombreNotes,
        List<NotesCoursDto> parCours) {

    /**
     * Notes obtenues dans un cours.
     *
     * @param moyenne moyenne du cours, {@code null} si aucune note n'y a été saisie
     */
    public record NotesCoursDto(
            Long coursId,
            String coursCode,
            String coursTitre,
            Double moyenne,
            List<NoteModuleDto> notes) {
    }
}
