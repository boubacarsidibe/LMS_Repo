package inpg.sn.lms.rendezvous.dto;

import java.time.LocalDateTime;

public record RendezVousDto(
        Long id,
        Long creneauId,
        LocalDateTime dateHeureDebut,
        LocalDateTime dateHeureFin,
        Long personnelId,
        String personnelNom,
        Long demandeurId,
        String demandeurNom,
        String modalite,
        String lieuOuLien,
        String motif,
        String statut,
        LocalDateTime dateHeureProposeeDebut,
        LocalDateTime dateHeureProposeeFin,
        String commentaireReponse,
        LocalDateTime dateCreation,
        LocalDateTime dateReponse) {
}
