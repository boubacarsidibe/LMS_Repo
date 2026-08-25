package inpg.sn.lms.espace_etudiant.dto;

public record NoteModuleDto(
        Long id,
        String module,
        double note,
        String commentaire,
        String dateEvaluation,
        Long coursId,
        String coursCode,
        String coursTitre,
        String formateurNom) {

}
