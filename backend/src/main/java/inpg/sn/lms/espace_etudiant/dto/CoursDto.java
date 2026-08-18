package inpg.sn.lms.espace_etudiant.dto;

import java.util.List;

public record CoursDto(
        Long id,
        String code,
        String titre,
        String description,
        String semestre,
        Long volumeHoraire,
        /** Poids du cours dans la moyenne generale. Vaut 1 par defaut. */
        double coefficient,
        long nbEtudiants,
        long nbRessources,
        int progression,
        String couleurBandeau,
        List<PromotionDto> promotions,
        /**
         * Programmes auxquels le cours est inscrit. Vide signale une rupture : sans
         * formation, aucune promotion ne le reçoit, donc aucun étudiant ne le voit.
         * C'est le rattachement réel — {@code promotions} n'en est que la dérivation.
         */
        List<FormationDto> formations,
        /**
         * Équipe pédagogique du cours. Vide signale une rupture : la table
         * {@code cours_formateur} commande tout le cloisonnement enseignant, donc un
         * cours sans formateur n'apparaît dans aucun espace formateur — ni supports,
         * ni séances, ni notes possibles.
         */
        List<FormateurBrefDto> formateurs,
        String imageUrl) {

}
