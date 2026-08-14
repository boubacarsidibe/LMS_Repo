package inpg.sn.lms.espace_etudiant.dto;

/** present = null tant que le formateur n'a pas encore pointe cet etudiant pour cette seance. */
public record PresenceEtudiantDto(Long etudiantId, String nom, String initiales, String couleurAvatar, Boolean present) {

}
