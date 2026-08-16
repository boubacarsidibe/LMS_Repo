package inpg.sn.lms.espace_etudiant.dto;

import java.util.List;

public record CoursDto(
        Long id,
        String code,
        String titre,
        String description,
        String semestre,
        Long volumeHoraire,
        long nbEtudiants,
        long nbRessources,
        int progression,
        String couleurBandeau,
        List<PromotionDto> promotions,
        String imageUrl) {

}
