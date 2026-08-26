package inpg.sn.lms.entreprise.dto;

import java.time.LocalDateTime;

public record MessageEntrepriseDto(
        Long id,
        Long entrepriseId,
        Long auteurId,
        String auteurNom,
        boolean auteurEntreprise,
        String contenu,
        LocalDateTime dateCreation) {
}
