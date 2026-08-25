package inpg.sn.lms.espace_etudiant.entity;

/**
 * Public visé par un sujet général du forum ({@code cours == null}).
 *
 * <p>N'a de sens que pour un sujet général : un sujet rattaché à un {@link Cours}
 * garde toujours {@link #ETUDIANT}, sa visibilité étant déjà entièrement décidée
 * par le cours (voir migration V13).
 */
public enum PorteeForumSujet {
    /** Comportement historique : ouvert à tout l'espace étudiant. */
    ETUDIANT,
    /** Espace alumni (TDR §C), distinct de l'espace étudiant. */
    ALUMNI
}
