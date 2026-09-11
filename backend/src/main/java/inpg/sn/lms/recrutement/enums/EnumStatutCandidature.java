package inpg.sn.lms.recrutement.enums;

public enum EnumStatutCandidature {
    BROUILLON,
    SOUMIS,
    EN_COURS_DE_PRESELECTION,
    REJETE,
    ADMIS1,
    ADMIS2,
    /**
     * Liste complémentaire : entretien passé, candidat retenu sur le fond mais en
     * attente qu'une place se libère (TDR §3.1.C, « listes retenus et en attente »).
     * Ni une admission ni un rejet — d'où un statut à part, atteint depuis ADMIS2 et
     * dont on ne sort que vers ADMIS3 (une place s'est libérée) ou REJETE.
     */
    LISTE_ATTENTE,
    ADMIS3
}
