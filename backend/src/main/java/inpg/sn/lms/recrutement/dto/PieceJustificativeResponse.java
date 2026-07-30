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
    private Long fichierStockeId;
    private String fichierNomOriginal;
}