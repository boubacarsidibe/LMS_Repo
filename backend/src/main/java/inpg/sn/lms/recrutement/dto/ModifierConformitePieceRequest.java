package inpg.sn.lms.recrutement.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Décision de contrôle d'une pièce justificative par le service admission. */
@Getter
@Setter
public class ModifierConformitePieceRequest {

    @NotNull
    private Boolean estConforme;

    /** Obligatoire lorsque la pièce est refusée ; invisible des autres candidats. */
    @Size(max = 500)
    private String motifNonConformite;
}
