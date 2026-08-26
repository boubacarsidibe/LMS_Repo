package inpg.sn.lms.recrutement.enums;

public enum EnumTypeDocument {
    CV,
    DIPLOME,
    RELEVE_NOTES,
    PIECE_IDENTITE,
    LETTRE_MOTIVATION;

    /** Libellé français, réutilisé partout où ce type est montré au candidat (emails, écrans admin). */
    public String libelle() {
        return switch (this) {
            case CV -> "CV";
            case DIPLOME -> "Diplôme";
            case RELEVE_NOTES -> "Relevé de notes";
            case PIECE_IDENTITE -> "Pièce d'identité";
            case LETTRE_MOTIVATION -> "Lettre de motivation";
        };
    }
}