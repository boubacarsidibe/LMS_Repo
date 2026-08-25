package inpg.sn.lms.espace_etudiant.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Relevé de notes tel que délivré, avec les valeurs figées à cette date.
 *
 * <p>À ne pas confondre avec {@link ReleveNotesDto}, qui est le relevé <em>courant</em>,
 * recalculé à chaque affichage. Celui-ci est un document : ses chiffres ne bougent plus,
 * même si un formateur corrige une note ensuite.
 *
 * @param reference identifiant du document, seul moyen de vérifier qu'il est authentique
 */
public record ReleveDelivreDto(
        Long id,
        String reference,
        Long etudiantId,
        String etudiantNom,
        String matricule,
        String promotionLibelle,
        String periode,
        LocalDate dateEmission,
        String delivrePar,
        Double moyenneGenerale,
        int nombreNotes,
        List<LigneDto> lignes) {

    public record LigneDto(
            String coursCode,
            String coursTitre,
            double coefficient,
            Double moyenne) {
    }
}
