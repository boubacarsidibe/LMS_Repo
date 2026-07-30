package inpg.sn.lms.recrutement.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntretienResponse {

    private Long id;
    private LocalDateTime dateHeure;
    private String lieuOuLien;
    private Float noteEntretien;
    private String appreciationGlobale;
    private Long candidatId;
}