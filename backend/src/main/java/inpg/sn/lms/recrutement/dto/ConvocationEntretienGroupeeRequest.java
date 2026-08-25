package inpg.sn.lms.recrutement.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Convocation groupée à un entretien (TDR : "mail groupé ou individuel pour les informer de l'avancée du dossier"). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConvocationEntretienGroupeeRequest {

    private List<Long> candidatIds;
    private LocalDateTime dateHeure;
    private String lieuOuLien;
}
