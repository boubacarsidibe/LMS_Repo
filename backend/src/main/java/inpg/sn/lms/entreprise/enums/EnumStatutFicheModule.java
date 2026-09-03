package inpg.sn.lms.entreprise.enums;

/**
 * Cycle de vie d'une Fiche Technique de Module (formation d'entreprise sur
 * mesure). La validation interne par le Conseil Pédagogique et Industriel se
 * fait hors LMS (comme la convention d'accréditation côté DRCQ) : ici seuls
 * comptent le brouillon de la DPI, sa transmission à l'entreprise et la
 * décision de celle-ci.
 */
public enum EnumStatutFicheModule {
    /** En cours de rédaction par le responsable pédagogique, invisible pour l'entreprise. */
    BROUILLON,
    /** Transmise à l'entreprise pour revue. */
    TRANSMISE,
    /** Acceptée par l'entreprise : la demande est clôturée. */
    ACCEPTEE,
    /** L'entreprise a demandé des ajustements (commentaire obligatoire). */
    AJUSTEMENTS_DEMANDES
}
