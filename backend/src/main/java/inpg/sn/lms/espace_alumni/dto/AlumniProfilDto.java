package inpg.sn.lms.espace_alumni.dto;

import java.time.LocalDate;

public record AlumniProfilDto(
        Long id,
        String nom,
        String prenom,
        String matricule,
        String promotionLibelle,
        String posteActuel,
        String entreprise,
        String secteurActivite,
        boolean visible,
        LocalDate dateCreation) {
}
