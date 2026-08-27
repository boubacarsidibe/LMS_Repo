package inpg.sn.lms.rendezvous.dto;

/** Profil consultable pour la prise de rendez-vous (TDR §J). */
public record PersonnelProfilDto(
        Long id,
        String nomAffiche,
        String role) {
}
