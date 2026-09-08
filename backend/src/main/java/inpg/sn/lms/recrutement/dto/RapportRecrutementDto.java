package inpg.sn.lms.recrutement.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Rapport qualité du recrutement : répartition des candidatures par étape du
 * workflow (voir {@code CandidatureService#estTransitionValide}), avec pour
 * chacune la liste nominative des candidats concernés. Sert à la fois l'écran
 * (JSON) et le PDF téléchargeable — aucun chiffre n'est recalculé côté PDF, il
 * ne fait que mettre en page ce DTO.
 *
 * <p>{@code BROUILLON} est volontairement absent : ce statut n'est jamais
 * atteint en pratique (voir le commentaire de {@code CandidatureService}).
 */
public record RapportRecrutementDto(
        int totalCandidatures,
        List<EtapeRapport> etapes) {

    public record EtapeRapport(
            String statut,
            String libelle,
            int nombre,
            List<CandidatRapport> candidats) {
    }

    public record CandidatRapport(
            String numeroDossier,
            String nom,
            String prenom,
            String email,
            String sessionLibelle,
            LocalDateTime dateSoumission,
            /** Renseigné uniquement pour l'étape REJETE. */
            String motifRejet) {
    }
}
