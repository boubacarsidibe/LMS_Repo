package inpg.sn.lms.accreditation.enums;

/** État d'une demande d'accréditation d'école de formation (TDR §F). */
public enum EnumStatutDemandeAccreditation {
    /** Déposée, pas encore prise en charge par un évaluateur. */
    SOUMISE,
    /** Prise en charge : un évaluateur consulte le dossier. */
    EN_COURS_EVALUATION,
    /** Décision favorable : l'école rejoint la liste publique des écoles accréditées. */
    ACCREDITE,
    /** Décision défavorable, avec motif obligatoire. */
    REJETE
}
