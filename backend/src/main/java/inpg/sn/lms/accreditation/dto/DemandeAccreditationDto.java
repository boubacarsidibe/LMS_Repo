package inpg.sn.lms.accreditation.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DemandeAccreditationDto(
        Long id,
        String statut,
        LocalDateTime dateSoumission,
        LocalDateTime dateDecision,
        String motifRejet,
        String commentaireEvaluation,
        Long ecoleId,
        String nomEcole,
        List<PieceAccreditationDto> pieces) {
}
