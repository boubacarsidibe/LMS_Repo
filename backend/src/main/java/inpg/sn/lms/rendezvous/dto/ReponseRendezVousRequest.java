package inpg.sn.lms.rendezvous.dto;

import java.time.LocalDateTime;

/**
 * {@code action} : ACCEPTER, REFUSER ou PROPOSER (TDR §J). Pour PROPOSER,
 * {@code nouvelleDateHeureDebut}/{@code nouvelleDateHeureFin} portent la date
 * alternative suggérée par le personnel.
 */
public record ReponseRendezVousRequest(
        String action,
        LocalDateTime nouvelleDateHeureDebut,
        LocalDateTime nouvelleDateHeureFin,
        String commentaire) {
}
