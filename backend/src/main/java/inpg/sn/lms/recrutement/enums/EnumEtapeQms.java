package inpg.sn.lms.recrutement.enums;

/**
 * Étapes de la procédure qualité QMS-009 « Admission des étudiants et apprenants »
 * qui portent sur un dossier de candidature.
 *
 * <p>Les étapes 1 (élaboration de la fiche du programme) et 2 (annonce des programmes
 * et critères) du document ne figurent pas ici : elles portent sur la session, pas sur
 * un dossier. {@code PUBLICATION} est réservée mais pas encore écrite — la forme exacte
 * de la publication des résultats reste à trancher par l'INPG.
 */
public enum EnumEtapeQms {

    /** Étape 3 — réception du dossier déposé par le candidat. */
    RECEPTION,

    /** Étape 3 — filtre initial : évaluation automatique des critères de la session. */
    FILTRE_INITIAL,

    /** Étape 4 — évaluation du dossier : convocation au test, puis à l'entretien. */
    EVALUATION,

    /** Étape 5 — sélection : décision d'admission définitive ou de rejet. */
    SELECTION,

    /** Étape 6 — inscription : ouverture de l'accès étudiant. */
    INSCRIPTION,

    /** Étape 6 — publication des résultats (non encore implémentée). */
    PUBLICATION
}
