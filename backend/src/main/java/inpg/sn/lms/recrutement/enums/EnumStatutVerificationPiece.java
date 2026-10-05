package inpg.sn.lms.recrutement.enums;

/**
 * État métier d'une pièce déposée par un candidat.
 *
 * <p>Le booléen historique {@code estConforme} ne distinguait pas une pièce qui
 * attend encore l'examen d'une pièce réellement refusée. Cette distinction est
 * indispensable : seule la seconde doit demander une action au candidat.
 */
public enum EnumStatutVerificationPiece {
    A_VERIFIER,
    CONFORME,
    A_REMPLACER
}
