package inpg.sn.lms.recrutement.dto;

import java.time.LocalDateTime;
import java.util.List;

import inpg.sn.lms.recrutement.enums.EnumStatutCandidature;
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
public class CandidatureResponse {

    private Long id;
    private String numeroDossier;
    private EnumStatutCandidature statut;
    private LocalDateTime dateSoumission;
    private String motifRejet;
    private Long candidatId;
    private String candidatNom;
    private String candidatPrenom;
    private String candidatEmail;
    private Long sessionCandidatureId;
    private String sessionCandidatureLibelle;
    private List<PieceJustificativeResponse> piecesJustificatives;
    private PaiementResponse paiement;
}