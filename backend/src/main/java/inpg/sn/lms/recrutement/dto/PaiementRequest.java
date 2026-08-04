package inpg.sn.lms.recrutement.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaiementRequest {

    private Long candidatureId;
    private BigDecimal montant;
    private String devise;
}