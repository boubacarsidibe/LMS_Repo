package inpg.sn.lms.accreditation.dto;

/** Dépôt ou mise à jour de l'avis de l'évaluateur courant sur une demande. {@code avis} : FAVORABLE ou DEFAVORABLE. */
public record AvisAccreditationRequest(String avis, String commentaire) {
}
