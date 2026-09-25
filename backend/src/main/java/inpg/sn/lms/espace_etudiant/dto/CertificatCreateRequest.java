package inpg.sn.lms.espace_etudiant.dto;

public record CertificatCreateRequest(Long etudiantId, String intituleFormation, Long cohorteId,
        String civilite, java.time.LocalDate dateNaissance, java.time.LocalDate dateDebut,
        java.time.LocalDate dateFin, String code, java.time.LocalDate dateDelivrance) {
}
