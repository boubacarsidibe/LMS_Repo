package inpg.sn.lms.recrutement.dto;

import java.time.LocalDateTime;

import inpg.sn.lms.recrutement.enums.EnumEtapeQms;
import inpg.sn.lms.recrutement.enums.EnumStatutCandidature;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Une ligne du journal d'étapes d'un dossier (procédure QMS-009). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EtapeDossierResponse {

    private Long id;
    private EnumEtapeQms etape;
    private EnumStatutCandidature statutResultant;
    private String auteurNom;
    private LocalDateTime dateEtape;
    private String commentaire;
}
