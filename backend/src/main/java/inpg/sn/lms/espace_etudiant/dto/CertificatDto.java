package inpg.sn.lms.espace_etudiant.dto;

public record CertificatDto(
        Long id,
        String reference,
        Long etudiantId,
        String etudiantNom,
        String etudiantMatricule,
        String intituleFormation,
        String cohorteLibelle,
        String dateEmission,
        String formateurNom) {

}
