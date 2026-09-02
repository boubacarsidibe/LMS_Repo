package inpg.sn.lms.recrutement.dto;

import inpg.sn.lms.recrutement.enums.EnumTypeCritere;
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
public class CritereAdmissionResponse {

    private Long id;
    private String libelle;
    private EnumTypeCritere type;
    private boolean obligatoire;
    private String options;
    private Integer valeurMinimale;
    private String valeurAttendue;
    private int ordre;
    private boolean actif;
}
