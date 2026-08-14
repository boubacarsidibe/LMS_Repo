package inpg.sn.lms.recrutement.dto;

/** Formation telle qu'affichée dans le catalogue public. */
public record FormationPubliqueDto(
        Long id,
        String code,
        String intitule,
        String description,
        String niveau,
        String domaine,
        String duree,
        String imageUrl) {
}
