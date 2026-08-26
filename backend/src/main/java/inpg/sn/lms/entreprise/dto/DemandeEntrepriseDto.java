package inpg.sn.lms.entreprise.dto;

import java.time.LocalDateTime;

public record DemandeEntrepriseDto(
        Long id,
        String type,
        String statut,
        Long entrepriseId,
        String raisonSociale,
        Long formationId,
        String intituleFormation,
        String description,
        Integer effectifConcerne,
        String datesSouhaitees,
        LocalDateTime dateSoumission,
        LocalDateTime dateReponse,
        String reponseEquipe) {
}
