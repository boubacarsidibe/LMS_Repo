package inpg.sn.lms.espace_etudiant.dto;

/**
 * Reponse de la verification publique d'un certificat.
 *
 * <p>Volontairement etroit : ni identifiant technique, ni matricule, ni
 * promotion, ni aucune donnee permettant de rebondir vers d'autres informations
 * de l'etudiant. Seul ce qui figure deja en clair sur le document imprime est
 * restitue — c'est l'objet meme d'une verification de certificat.
 */
public record CertificatVerificationDto(
        String reference,
        String etudiantNomComplet,
        String intituleFormation,
        String dateEmission,
        String formateurNom,
        boolean verifiable,
        boolean valide) {

}
