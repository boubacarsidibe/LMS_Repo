package inpg.sn.lms.espace_etudiant.dto;

public record CertificatCreateRequest(Long etudiantId, String intituleFormation, Long cohorteId) {

}
