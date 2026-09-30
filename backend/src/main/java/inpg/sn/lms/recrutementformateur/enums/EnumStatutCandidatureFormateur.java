package inpg.sn.lms.recrutementformateur.enums;

/**
 * Suivi d'une candidature de formateur, en trois états visibles du candidat
 * (décision produit) : dossier reçu, en cours de traitement, terminé.
 */
public enum EnumStatutCandidatureFormateur {
    /** Dossier déposé, accusé de réception envoyé. */
    DOSSIER_RECU,
    /** Pris en charge : tri et instruction par le responsable admission. */
    EN_COURS_DE_TRAITEMENT,
    /** Instruction close : une décision (RETENU / NON_RETENU) est rendue. */
    TERMINE
}
