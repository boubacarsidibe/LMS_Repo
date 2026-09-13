package inpg.sn.lms.recrutement.dto;

import inpg.sn.lms.recrutement.enums.EnumTypeDocument;
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
public class PieceJustificativeResponse {

    private Long id;
    private EnumTypeDocument typeDocument;
    private boolean estConforme;
    private String description;
    private Long fichierStockeId;
    private String fichierNomOriginal;
    /** Moyenne declaree par le candidat pour un releve de notes (sur 20), null sinon. */
    private Double moyenneDeclaree;
}