package inpg.sn.lms.accreditation.enums;

/** État d'une demande d'accréditation d'un programme de formation (Cadre de référence DRCQ §IV). */
public enum EnumStatutDemandeAccreditation {
    /** Déposée, pas encore prise en charge par un évaluateur. */
    SOUMISE,
    /** Prise en charge : un évaluateur consulte le dossier. */
    EN_COURS_EVALUATION,
    /** Accréditation pleine : le programme rejoint la liste publique des programmes accrédités (Cadre §IV.8). */
    ACCREDITE,
    /**
     * Accréditation conditionnelle : décision favorable assortie d'un plan
     * d'actions que l'établissement doit mettre en œuvre (Cadre §IV.8). Le
     * programme figure sur la liste publique, signalé « sous réserve ».
     */
    ACCREDITE_SOUS_RESERVE,
    /** Refus d'accréditation, avec motif obligatoire. */
    REJETE
}
