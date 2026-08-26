package inpg.sn.lms.accreditation.dto;

import java.time.LocalDateTime;

/** Avis d'un évaluateur sur une demande, tel que consulté par l'espace évaluateur. */
public record AvisAccreditationDto(
        Long id,
        Long evaluateurId,
        String evaluateurNom,
        String avis,
        String commentaire,
        LocalDateTime dateAvis) {
}
