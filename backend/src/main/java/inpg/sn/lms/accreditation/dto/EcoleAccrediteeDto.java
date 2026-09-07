package inpg.sn.lms.accreditation.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Programme accrédité, vue publique (TDR §F : « base de données des écoles
 * accréditées », « processus transparent » ; Cadre de référence DRCQ §VI :
 * label « Programme accrédité par l'INPG »). Volontairement restreint : ni
 * adresse, ni téléphone, ni site web — un tiers vérifie qu'un programme est
 * accrédité et jusqu'à quand, il n'a pas besoin des coordonnées complètes.
 */
public record EcoleAccrediteeDto(
        String nomEcole,
        String intituleProgramme,
        String niveauProgramme,
        LocalDateTime dateAccreditation,
        LocalDate dateExpiration,
        boolean sousReserve) {
}
