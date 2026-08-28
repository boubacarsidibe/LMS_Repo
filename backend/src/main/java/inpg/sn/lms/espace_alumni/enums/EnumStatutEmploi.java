package inpg.sn.lms.espace_alumni.enums;

/**
 * Statut d'une ligne de l'historique professionnel d'un alumni (TDR §C, suivi
 * professionnel). Distinct de {@code dateFin == null} : celle-ci indique
 * seulement qu'une situation est en cours, pas sa nature — utile pour la
 * répartition affichée côté pédagogie (en emploi / en recherche / etc.).
 */
public enum EnumStatutEmploi {
    EN_EMPLOI,
    RECHERCHE_EMPLOI,
    ENTREPRENARIAT,
    POURSUITE_ETUDES,
    AUTRE
}
