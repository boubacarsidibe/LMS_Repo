package inpg.sn.lms.recrutement.enums;

/** Un type par email transactionnel envoyé aux candidats, personnalisable depuis le back-office. */
public enum EnumTypeNotification {
    VALIDATION_DOSSIER,
    REJET_DOSSIER,
    PAIEMENT_SUCCES,
    CONVOCATION_TEST,
    CONVOCATION_ENTRETIEN,
    ADMISSION_TEST,
    ADMISSION_ENTRETIEN,
    ADMISSION_FINALE,
    NON_ADMISSION,
    DOCUMENT_CONFORME,
    DOCUMENT_NON_CONFORME,
    RAPPEL_DOSSIER_INCOMPLET,
    RAPPEL_PAIEMENT
}
