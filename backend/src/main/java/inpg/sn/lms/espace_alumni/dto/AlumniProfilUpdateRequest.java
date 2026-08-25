package inpg.sn.lms.espace_alumni.dto;

public record AlumniProfilUpdateRequest(
        String posteActuel,
        String entreprise,
        String secteurActivite,
        boolean visible) {
}
