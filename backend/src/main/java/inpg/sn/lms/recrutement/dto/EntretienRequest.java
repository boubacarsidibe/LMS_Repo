package inpg.sn.lms.recrutement.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EntretienRequest {

    private LocalDateTime dateHeure;
    private String lieuOuLien;
    private Float noteEntretien;
    private String appreciationGlobale;
    private Long candidatId;
}