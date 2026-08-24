package inpg.sn.lms.accreditation.dto;

import java.time.LocalDate;

public record EcoleFormationDto(
        Long id,
        String nomEcole,
        String adresse,
        String telephone,
        String siteWeb,
        LocalDate dateCreation) {
}
