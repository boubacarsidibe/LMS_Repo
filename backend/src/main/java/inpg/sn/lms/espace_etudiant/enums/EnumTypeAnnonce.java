package inpg.sn.lms.espace_etudiant.enums;

/**
 * Nature d'une communication descendante de l'INPG vers les étudiants.
 *
 * <p>Le cahier des charges (§3.2) distingue explicitement trois natures :
 * « les annonces, messages et mémos en provenance de l'INPG ».
 */
public enum EnumTypeAnnonce {

    /** Information générale et publique adressée à une promotion ou à tous. */
    ANNONCE,

    /** Communication adressée, à portée plus directe qu'une annonce. */
    MESSAGE,

    /** Note administrative de référence, destinée à être conservée. */
    MEMO
}
