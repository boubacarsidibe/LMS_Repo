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
        /**
         * Équipe pédagogique du cours. Vide signale une rupture : la table
         * {@code cours_formateur} commande tout le cloisonnement enseignant, donc un
         * cours sans formateur n'apparaît dans aucun espace formateur — ni supports,
         * ni séances, ni notes possibles.
         */
        List<FormateurBrefDto> formateurs,
        String imageUrl) {

}
