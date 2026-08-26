package inpg.sn.lms.espace_etudiant.dto;

public record CertificatDto(
        Long id,
        String reference,
        Long etudiantId,
        String etudiantNom,
        String etudiantMatricule,
        String intituleFormation,
        String promotionLibelle,
        String dateEmission,
        String formateurNom) {

}
