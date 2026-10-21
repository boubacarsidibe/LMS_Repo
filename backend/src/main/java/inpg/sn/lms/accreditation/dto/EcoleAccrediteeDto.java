package inpg.sn.lms.accreditation.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Programme accrédité, vue publique (TDR §F : « base de données des écoles
 * accréditées », « processus transparent » ; Cadre de référence DRCQ §VI :
 * label « Programme accrédité par l'INPG »). Volontairement restreint : ni
 * adresse, ni téléphone, ni site web — un tiers vérifie qu'un programme est
 * accrédité et jusqu'à quand, il n'a pas besoin des coordonnées complètes.
 *
 * <p>{@code referenceAttestation} relie chaque entrée à sa fiche détaillée / vérification publique
 * ({@code /verification-accreditation/<référence>}, voir {@code AttestationAccreditationPublicController}) —
 * nul seulement pour une accréditation décidée avant cette fonctionnalité et jamais encore consultée
 * depuis l'espace école (référence générée à la volée au premier téléchargement de l'attestation).
 */
public record EcoleAccrediteeDto(
        String nomEcole,
        String intituleProgramme,
        String niveauProgramme,
        LocalDateTime dateAccreditation,
        LocalDate dateExpiration,
        boolean sousReserve,
        String referenceAttestation) {
}
