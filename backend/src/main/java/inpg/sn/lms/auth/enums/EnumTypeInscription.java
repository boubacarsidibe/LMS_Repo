package inpg.sn.lms.auth.enums;

/**
 * Intention déclarée à l'inscription publique : distingue dès le formulaire
 * un futur candidat, un représentant d'école venant déposer une demande
 * d'accréditation, et désormais une entreprise venant soumettre un besoin de
 * formation. Ne remplace pas l'attribution de rôle métier (ROLE_CANDIDAT/
 * ROLE_ECOLE_FORMATION/ROLE_ENTREPRISE), qui reste accordée uniquement à la
 * soumission réelle (CandidatController/EcoleAccreditationController/
 * EntrepriseController) — ce champ ne fait qu'orienter l'utilisateur vers le
 * bon point de départ juste après inscription.
 */
public enum EnumTypeInscription {
    CANDIDAT,
    ECOLE,
    ENTREPRISE
}
