package inpg.sn.lms.recrutementformateur.enums;

/**
 * Types de pièces d'un dossier de candidature formateur. {@code CV} et
 * {@code DIPLOME} sont requis ; {@code ATTESTATION_EXPERIENCE} et
 * {@code PIECE_IDENTITE} recommandés ; {@code AUTRE} facultatif et multiple.
 */
public enum EnumTypeDocumentCandidatureFormateur {
    CV,
    DIPLOME,
    ATTESTATION_EXPERIENCE,
    PIECE_IDENTITE,
    AUTRE
}
