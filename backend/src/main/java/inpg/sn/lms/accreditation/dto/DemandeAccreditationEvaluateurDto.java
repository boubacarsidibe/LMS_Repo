package inpg.sn.lms.accreditation.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Demande d'accréditation telle que vue par l'espace évaluateur : porte en plus
 * la liste des avis déjà déposés (TDR §F, décision collégiale) et le référentiel
 * appliqué au dossier, absents du DTO partagé avec l'espace école et la page
 * publique — une école ne doit pas voir les avis individuels des évaluateurs
 * avant la décision finale.
 */
public record DemandeAccreditationEvaluateurDto(
        Long id,
        String statut,
        LocalDateTime dateSoumission,
        LocalDateTime dateDecision,
        LocalDate dateExpiration,
        String motifRejet,
        String commentaireEvaluation,
        String planActions,
        Long ecoleId,
        String nomEcole,
        String adresse,
        String telephone,
        String siteWeb,
        String emailEcole,
        String intituleProgramme,
        String niveauProgramme,
        String typeStructure,
        Integer effectifCohorte,
        LocalDate dateDebutCohorte,
        List<PieceAccreditationDto> pieces,
        List<AvisAccreditationDto> avis,
        /** Référentiel actif au moment de l'évaluation, trié par ordre. */
        List<CritereAccreditationDto> criteresActifs,
        /** Grille de l'évaluateur connecté sur ce dossier (une entrée par critère jugé). */
        List<EvaluationCritereDto> maGrille) {
}
