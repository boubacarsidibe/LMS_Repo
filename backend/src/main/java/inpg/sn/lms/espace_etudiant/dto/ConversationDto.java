package inpg.sn.lms.espace_etudiant.dto;

public record ConversationDto(
        Long id,
        Long autrePersonneId,
        String autrePersonneNom,
        String dernierMessage,
        String dateDernierMessage,
        long nonLus) {
}
