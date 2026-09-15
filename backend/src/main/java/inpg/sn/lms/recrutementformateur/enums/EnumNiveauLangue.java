package inpg.sn.lms.recrutementformateur.enums;

/**
 * Niveau de maîtrise déclaré pour une langue du dossier de candidature. Seul
 * champ de l'enrichissement du dossier (V39) assez borné pour mériter un enum
 * plutôt qu'une chaîne libre — contrairement au reste (nationalité, niveau
 * d'étude…), qui suit la convention {@code InfosPersonnellesDto}.
 */
public enum EnumNiveauLangue {
    DEBUTANT,
    INTERMEDIAIRE,
    AVANCE,
    COURANT,
    LANGUE_MATERNELLE
}
