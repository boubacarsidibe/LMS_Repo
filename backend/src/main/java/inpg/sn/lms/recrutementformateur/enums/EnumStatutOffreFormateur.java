package inpg.sn.lms.recrutementformateur.enums;

/** Cycle de vie d'une offre de recrutement de formateur. */
public enum EnumStatutOffreFormateur {
    /** En cours de rédaction par le responsable admission, invisible du public. */
    BROUILLON,
    /** Publiée : visible sur la vitrine, ouverte aux candidatures. */
    PUBLIEE,
    /** Clôturée : n'accepte plus de nouvelle candidature. */
    CLOTUREE
}
