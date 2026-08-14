package inpg.sn.lms.espace_etudiant.dto;

public record PresenceHistoriqueDto(Long id, String date, String coursCode, String coursTitre, String seanceTitre, boolean present) {

}
