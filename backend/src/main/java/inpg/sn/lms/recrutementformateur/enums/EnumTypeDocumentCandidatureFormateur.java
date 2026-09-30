package inpg.sn.lms.recrutementformateur.enums;

/**
 * Types de pièces d'un dossier de candidature formateur. {@code CV} et
 * {@code DIPLOME} sont requis ; {@code ATTESTATION_EXPERIENCE},
 * {@code PIECE_IDENTITE} et {@code LETTRE_MOTIVATION} recommandés ; {@code AUTRE}
 * facultatif et multiple.
 *
 * <p>{@code LETTRE_MOTIVATION} remplace l'ancien champ texte libre
 * {@code CandidatureFormateur.lettreMotivation} (retour de recette du
 * 2026-09-20 : un candidat rédige et met en forme sa lettre dans son propre
 * traitement de texte, pas dans un {@code <textarea>} de formulaire web).
 */
public enum EnumTypeDocumentCandidatureFormateur {
    CV,
    DIPLOME,
    ATTESTATION_EXPERIENCE,
    PIECE_IDENTITE,
    LETTRE_MOTIVATION,
    AUTRE
}
