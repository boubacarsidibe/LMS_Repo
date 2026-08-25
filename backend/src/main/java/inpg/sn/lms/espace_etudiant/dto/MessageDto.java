package inpg.sn.lms.espace_etudiant.dto;

public record MessageDto(
        Long id,
        String auteur,
        String initiales,
        String contenu,
        String date,
        String pieceJointeNom) {

}
