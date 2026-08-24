package inpg.sn.lms.accreditation.dto;

public record CritereAccreditationDto(
        Long id,
        String libelle,
        String description,
        boolean actif) {
}
