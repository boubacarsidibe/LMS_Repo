package inpg.sn.lms.accreditation.enums;

/**
 * Niveau du programme de formation soumis à l'accréditation DRCQ.
 *
 * <p>Le Référentiel d'accréditation (§3.1) distingue explicitement les
 * programmes « technicien » et « ingénierie », qui n'ont pas le même quota
 * d'heures de simulation en laboratoire (20 h vs 70 h). Les autres niveaux
 * couvrent les cursus universitaires et les formations qualifiantes courtes.
 */
public enum EnumNiveauProgramme {
    TECHNICIEN,
    INGENIEUR,
    LICENCE,
    MASTER,
    MASTERE_SPECIALISE,
    CERTIFICAT,
    AUTRE
}
