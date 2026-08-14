package inpg.sn.lms.espace_etudiant.dto;

public record EtudiantSuiviDto(
        Long id,
        String nom,
        String cohorte,
        String derniereConnexion,
        int progression,
        double noteMoyenne,
        String statut,
        String initiales,
        String couleurAvatar,
        String matricule) {
}
