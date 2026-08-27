package inpg.sn.lms.rendezvous.dto;

import java.time.LocalDateTime;

public record CreneauCreateRequest(
        LocalDateTime dateHeureDebut,
        LocalDateTime dateHeureFin) {
}
