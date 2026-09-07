package inpg.sn.lms.accreditation.dto;

public record CritereAccreditationDto(
        Long id,
        String code,
        String section,
        String libelle,
        String description,
        int ordre,
        boolean actif) {
}
