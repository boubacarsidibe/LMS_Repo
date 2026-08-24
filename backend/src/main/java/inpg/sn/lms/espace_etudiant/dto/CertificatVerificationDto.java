package inpg.sn.lms.espace_etudiant.dto;

/**
 * Réponse de l'endpoint public de vérification d'un certificat (flashé depuis le
 * QR code). Volontairement plus restreinte que {@link CertificatDto} : ni
 * identifiant technique, ni matricule — seules les informations nécessaires pour
 * confirmer l'authenticité sont exposées sans authentification.
 */
public record CertificatVerificationDto(
        String reference,
        String etudiantNom,
        String intituleFormation,
        String promotionLibelle,
        String dateEmission,
        String formateurNom) {

}
