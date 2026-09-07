package inpg.sn.lms.accreditation.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record DemandeAccreditationDto(
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
        String intituleProgramme,
        String niveauProgramme,
        String typeStructure,
        Integer effectifCohorte,
        LocalDate dateDebutCohorte,
        List<PieceAccreditationDto> pieces) {
}
