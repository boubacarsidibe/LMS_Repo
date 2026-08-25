package inpg.sn.lms.espace_etudiant.dto;

public record RetourEvaluationDto(
        Long id,
        int note,
        String commentaire,
        String date,
        Long coursId,
        String cours,
        String statut,
        boolean epingle) {
}
