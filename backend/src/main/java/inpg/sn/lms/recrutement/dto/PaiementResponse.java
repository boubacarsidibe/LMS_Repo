package inpg.sn.lms.recrutement.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import inpg.sn.lms.recrutement.enums.EnumStatutPaiement;
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
public class PaiementResponse {

    private Long id;
    private String referenceTransaction;
    private BigDecimal montant;
    private String devise;
    private EnumStatutPaiement statut;
    private LocalDateTime datePaiement;
}