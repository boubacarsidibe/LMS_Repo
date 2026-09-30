package inpg.sn.lms.espace_etudiant.dto;

public record MessagePriveDto(
        Long id,
        String auteurNom,
        boolean estAuteur,
        String contenu,
        String dateEnvoi,
        String pieceJointeNom) {
}
