package inpg.sn.lms.recrutement.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Convocation groupée à un test académique (TDR : "mail de convocation aux tests groupé ou individuel"). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConvocationTestGroupeeRequest {

    private List<Long> candidatIds;
    private String nomEpreuve;
    private Float coefficient;
    private LocalDateTime dateEpreuve;
}
