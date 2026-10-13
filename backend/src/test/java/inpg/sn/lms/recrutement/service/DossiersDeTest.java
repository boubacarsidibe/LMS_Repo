package inpg.sn.lms.recrutement.service;

import java.time.LocalDateTime;

import inpg.sn.lms.auth.entity.Candidat;
import inpg.sn.lms.recrutement.entity.Candidature;
import inpg.sn.lms.recrutement.entity.FichierStocke;
import inpg.sn.lms.recrutement.entity.PieceJustificative;
import inpg.sn.lms.recrutement.enums.EnumStatutVerificationPiece;
import inpg.sn.lms.recrutement.enums.EnumTypeDocument;

/** Fabrique de dossiers pour les tests : un dossier « complet » au sens de {@link CompletudeDossier}. */
final class DossiersDeTest {

    private DossiersDeTest() {
    }

    /** Pièce déposée à l'instant donné (c'est ce que lit la détection d'une pièce remplacée). */
    static PieceJustificative piece(EnumTypeDocument type, EnumStatutVerificationPiece statut, LocalDateTime deposeLe,
            LocalDateTime verifieLe) {
        PieceJustificative p = new PieceJustificative();
        p.setTypeDocument(type);
        p.setStatutVerification(statut);
        p.setDateVerification(verifieLe);
        FichierStocke f = new FichierStocke();
        f.setDateUpload(deposeLe);
        p.setFichierStocke(f);
        return p;
    }

    static PieceJustificative piece(EnumTypeDocument type) {
        return piece(type, EnumStatutVerificationPiece.A_VERIFIER, null, null);
    }

    /** Ajoute au dossier les quatre pièces exigées d'un candidat de niveau Licence. */
    static Candidature completer(Candidature c) {
        if (c.getCandidat() == null) {
            c.setCandidat(new Candidat());
        }
        c.getCandidat().setNiveauEtude("Licence (L3)");
        c.getPiecesJustificatives().clear();
        for (EnumTypeDocument type : new EnumTypeDocument[] { EnumTypeDocument.PIECE_IDENTITE, EnumTypeDocument.CV,
                EnumTypeDocument.RELEVE_NOTES, EnumTypeDocument.DIPLOME }) {
            c.getPiecesJustificatives().add(piece(type));
        }
        return c;
    }
}
