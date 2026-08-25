package inpg.sn.lms.accreditation.dto;

import java.time.LocalDateTime;

/**
 * École accréditée, vue publique (TDR §F : « base de données des écoles
 * accréditées », « processus transparent »). Volontairement restreint : ni
 * adresse, ni téléphone, ni site web — un tiers vérifie qu'une école est
 * accréditée, il n'a pas besoin d'en obtenir les coordonnées complètes.
 */
public record EcoleAccrediteeDto(
        String nomEcole,
        LocalDateTime dateAccreditation) {
}
