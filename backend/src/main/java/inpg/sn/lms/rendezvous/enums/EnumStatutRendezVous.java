package inpg.sn.lms.rendezvous.enums;

/**
 * Cycle de vie d'une demande de rendez-vous (TDR §J : « accepter, refuser, ou
 * proposer une autre date »).
 */
public enum EnumStatutRendezVous {
    /** Déposée, en attente de réponse du membre du personnel. */
    EN_ATTENTE,
    ACCEPTE,
    REFUSE,
    /** Le personnel a proposé une autre date ; en attente de la réponse du demandeur. */
    PROPOSITION_AUTRE_DATE,
    /** Le demandeur a annulé sa propre demande avant toute réponse. */
    ANNULE
}
