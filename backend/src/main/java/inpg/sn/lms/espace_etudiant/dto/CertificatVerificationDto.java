package inpg.sn.lms.espace_etudiant.dto;

/**
 * Reponse de la verification publique d'un certificat.
 *
 * <p>Volontairement etroit : ni identifiant technique, ni matricule, ni
 * promotion, ni aucune donnee permettant de rebondir vers d'autres informations
 * de l'etudiant. Seul ce qui figure deja en clair sur le document imprime est
 * restitue — c'est l'objet meme d'une verification de certificat. La periode
 * et le code y figurent : ce sont les champs qui different d'un certificat a
 * l'autre, donc ceux qu'un faussaire modifierait sur un vrai document. La date
 * de naissance, elle aussi imprimee, n'y est en revanche jamais restituee :
 * c'est une donnee d'identite, qu'une page ouverte a tous n'a pas a exposer.
 */
public record CertificatVerificationDto(
        String reference,
        String etudiantNomComplet,
        String intituleFormation,
        String periode,
        String dateEmission,
        String formateurNom,
        String codeCertificat,
        boolean verifiable,
        boolean valide) {

}
