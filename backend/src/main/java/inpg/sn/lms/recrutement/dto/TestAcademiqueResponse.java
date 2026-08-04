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
public class TestAcademiqueResponse {

    private Long id;
    private String nomEpreuve;
    private Float coefficient;
    private Float noteObtenue;
    private LocalDateTime dateEpreuve;
    private Long candidatId;
}