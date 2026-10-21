package inpg.sn.lms.accreditation.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Réponse de la vérification publique d'une attestation d'accréditation, sur
 * le même principe restreint que {@code CertificatVerificationDto} : seul ce
 * qui figure déjà en clair sur le document imprimé est restitué (nom de
 * l'école, programme, décision) — jamais l'adresse, le téléphone ni le plan
 * d'actions d'une accréditation sous réserve, qui n'ont pas leur place sur une
 * page ouverte à tous.
 */
public record AttestationAccreditationVerificationDto(
        String reference,
        String nomEcole,
        String intituleProgramme,
        String niveauProgramme,
        LocalDateTime dateDecision,
        LocalDate dateExpiration,
        boolean sousReserve,
        boolean verifiable,
        boolean valide) {
}
