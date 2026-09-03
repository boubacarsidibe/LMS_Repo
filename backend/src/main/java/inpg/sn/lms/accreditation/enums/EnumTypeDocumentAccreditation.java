package inpg.sn.lms.accreditation.enums;

/**
 * Types de pièces attendues dans un dossier d'accréditation.
 *
 * <p>Les cinq premiers types viennent du TDR §F (« curriculum, politique
 * qualité, programme de formation, qualifications des formateurs, équipements
 * disponibles »). Les suivants sont ajoutés d'après le Cadre de référence DRCQ
 * §IV.1 et §IV.4 : engagement à respecter le référentiel, preuve de
 * reconnaissance officielle (statut légal / ANAQ-SUP), rapport
 * d'auto-évaluation du programme, organigramme, CV et certifications des
 * formateurs, résultats des cohortes précédentes, règlement intérieur.
 * {@code AUTRE} reste facultatif et multiple.
 */
public enum EnumTypeDocumentAccreditation {
    CURRICULUM,
    POLITIQUE_QUALITE,
    PROGRAMME_FORMATION,
    QUALIFICATIONS_FORMATEURS,
    EQUIPEMENTS_DISPONIBLES,
    ENGAGEMENT_REFERENTIEL,
    PREUVE_RECONNAISSANCE,
    RAPPORT_AUTO_EVALUATION,
    ORGANIGRAMME,
    CV_CERTIFICATIONS_FORMATEURS,
    RESULTATS_COHORTES_PRECEDENTES,
    REGLEMENT_INTERIEUR,
    AUTRE
}
