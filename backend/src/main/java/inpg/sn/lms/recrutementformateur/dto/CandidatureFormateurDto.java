package inpg.sn.lms.recrutementformateur.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Candidature de formateur telle que servie au candidat (ses propres dossiers)
 * et au responsable admission (instruction). Porte l'offre visée, le suivi et
 * les pièces.
 */
public record CandidatureFormateurDto(
        Long id,
        Long offreId,
        String intituleOffre,
        String typeContratOffre,
        String nom,
        String prenom,
        String email,
        String telephone,
        String diplomePrincipal,
        Integer anneesExperience,
        String lettreMotivation,
        String statut,
        String decision,
        String motifDecision,
        LocalDateTime dateSoumission,
        LocalDateTime dateDecision,
        boolean formateurCree,
        List<PieceCandidatureFormateurDto> pieces) {
}
