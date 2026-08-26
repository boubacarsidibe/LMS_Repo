package inpg.sn.lms.accreditation.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Demande d'accréditation telle que vue par l'espace évaluateur : porte en plus
 * la liste des avis déjà déposés (TDR §F, décision collégiale), absente du DTO
 * partagé avec l'espace école et la page publique — une école ne doit pas voir
 * les avis individuels des évaluateurs avant la décision finale.
 */
public record DemandeAccreditationEvaluateurDto(
        Long id,
        String statut,
        LocalDateTime dateSoumission,
        LocalDateTime dateDecision,
        String motifRejet,
        String commentaireEvaluation,
        Long ecoleId,
        String nomEcole,
        List<PieceAccreditationDto> pieces,
        List<AvisAccreditationDto> avis) {
}
