package inpg.sn.lms.espace_etudiant.dto;

import java.util.List;

public record SujetDto(
        Long id,
        String auteur,
        String initiales,
        String couleurAvatar,
        String couleurTexte,
        String cours,
        String module,
        String titre,
        String apercu,
        String date,
        String statut,
        List<MessageDto> messages) {

}
