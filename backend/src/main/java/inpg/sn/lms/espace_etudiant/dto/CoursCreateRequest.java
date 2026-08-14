package inpg.sn.lms.espace_etudiant.dto;

import java.util.List;

public record CoursCreateRequest(
        String code,
        String titre,
        String semestre,
        String description,
        Long volumeHoraire,
        List<Long> cohorteIds) {
}
