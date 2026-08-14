package inpg.sn.lms.espace_etudiant.dto;

import java.time.LocalDateTime;

import inpg.sn.lms.espace_etudiant.enums.EnumTypeAnnonce;

/** Communication de l'INPG telle que présentée à l'étudiant. */
public record AnnonceDto(
        Long id,
        String titre,
        String contenu,
        EnumTypeAnnonce type,
        LocalDateTime datePublication,
        String auteur,
        String promotionLibelle,
        boolean epinglee,
        boolean lue) {
}
