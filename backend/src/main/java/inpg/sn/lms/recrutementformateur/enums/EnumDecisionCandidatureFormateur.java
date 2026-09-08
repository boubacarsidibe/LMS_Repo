package inpg.sn.lms.recrutementformateur.enums;

/** Issue d'une candidature de formateur, posée au passage en {@code TERMINE}. */
public enum EnumDecisionCandidatureFormateur {
    /** Candidat retenu : bascule du compte en formateur (QMS-007 §4). */
    RETENU,
    /** Candidat non retenu : motif obligatoire. */
    NON_RETENU
}
