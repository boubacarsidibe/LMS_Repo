package inpg.sn.lms.recrutement.dto;

import java.time.LocalDateTime;
import java.util.UUID;

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
    private boolean confirmeParCandidat;
    private String motifReport;
    private Long candidatId;
    private String candidatNom;
    private String candidatPrenom;
    private String numeroDossier;
    private UUID groupeEntretienId;
}
