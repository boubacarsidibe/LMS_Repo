package inpg.sn.lms.espace_etudiant.dto;

public record NoteModuleCreateRequest(Long coursId, String module, double note, String commentaire) {

}
