package inpg.sn.lms.espace_etudiant.dto;

import java.util.List;

/**
 * Suivi qualité de l'enseignement (TDR §3.7), vu au niveau de l'établissement.
 *
 * <p>Les avis des étudiants existaient déjà en base, mais seul le formateur
 * concerné voyait les siens : personne ne disposait d'une vue d'ensemble. C'est
 * pourtant elle qui permet de repérer un cours en difficulté.
 *
 * @param moyenneGenerale moyenne de tous les avis, {@code null} s'il n'y en a aucun
 * @param coursASurveiller nombre de cours dont la moyenne est au plus de 3 sur 5
 */
public record QualiteCoursDto(
        Double moyenneGenerale,
        int nombreAvis,
        int coursEvalues,
        int coursASurveiller,
        List<LigneQualite> parCours) {

    /**
     * @param moyenne          moyenne du cours, {@code null} si aucun avis
     * @param actionsRequises  avis au plus égaux à 3 sur 5, qui appellent une réaction
     */
    public record LigneQualite(
            Long coursId,
            String coursCode,
            String coursTitre,
            Double moyenne,
            int nombreAvis,
            int actionsRequises,
            List<String> derniersCommentaires) {
    }
}
