package inpg.sn.lms.recrutement.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CandidatureRequest {

    private Long sessionCandidatureId;
    private String informationsPersonnelles;
    private String informationsAcademiques;
    private String informationsProfessionnelles;
}