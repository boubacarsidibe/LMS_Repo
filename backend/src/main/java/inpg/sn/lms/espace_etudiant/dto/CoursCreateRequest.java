package inpg.sn.lms.espace_etudiant.dto;

public record CoursCreateRequest(
        String code,
        String titre,
        String semestre,
        String description,
        Long volumeHoraire) {
}
