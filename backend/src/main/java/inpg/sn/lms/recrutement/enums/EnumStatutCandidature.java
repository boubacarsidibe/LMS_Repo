package inpg.sn.lms.recrutement.enums;

/** Etapes explicites du parcours de recrutement d'une candidature. */
public enum EnumStatutCandidature {
    BROUILLON,
    SOUMIS,
    INCOMPLET,
    DOSSIER_VALIDE,
    DOSSIER_REJETE,
    PRESELECTIONNE,
    PAIEMENT_EN_ATTENTE,
    PAIEMENT_CONFIRME,
    CONVOQUE_TEST,
    TEST_REUSSI,
    TEST_ECHOUE,
    ABSENT_TEST,
    CONVOQUE_ENTRETIEN,
    ENTRETIEN_EFFECTUE,
    ABSENT_ENTRETIEN,
    ADMIS,
    LISTE_ATTENTE,
    NON_ADMIS,
    INSCRIPTION_FINALISEE,
    DESISTEMENT
}
