package inpg.sn.lms.rendezvous.dto;

import java.time.LocalDateTime;

public record CreneauDto(
        Long id,
        Long personnelId,
        String personnelNom,
        LocalDateTime dateHeureDebut,
        LocalDateTime dateHeureFin,
        boolean reserve) {
}
