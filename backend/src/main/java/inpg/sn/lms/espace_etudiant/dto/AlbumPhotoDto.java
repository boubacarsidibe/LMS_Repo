package inpg.sn.lms.espace_etudiant.dto;

import java.time.LocalDate;

/** Album de la photothèque, résumé pour l'affichage en grille. */
public record AlbumPhotoDto(
        Long id,
        String titre,
        String description,
        LocalDate dateEvenement,
        String promotionLibelle,
        long nbPhotos,
        String couvertureUrl) {
}
