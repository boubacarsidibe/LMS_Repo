package inpg.sn.lms.recrutement.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import inpg.sn.lms.auth.entity.Candidat;
import inpg.sn.lms.recrutement.dto.DocumentAttenduDto;
import inpg.sn.lms.recrutement.dto.DocumentAttenduDto.Etat;
import inpg.sn.lms.recrutement.entity.Candidature;
import inpg.sn.lms.recrutement.entity.PieceJustificative;
import inpg.sn.lms.recrutement.enums.EnumStatutVerificationPiece;
import inpg.sn.lms.recrutement.enums.EnumTypeDocument;

/**
 * La liste des pièces exigées existait en trois copies (formulaire, page de suivi, relance par e-mail) qui avaient
 * divergé : le suivi disait « tous les documents requis téléchargés » pendant que la relance réclamait le diplôme.
 * Ces tests figent la définition unique.
 */
class CompletudeDossierTest {

    private Candidature dossier(String niveau, PieceJustificative... pieces) {
        Candidat candidat = new Candidat();
        candidat.setNiveauEtude(niveau);
        Candidature c = new Candidature();
        c.setCandidat(candidat);
        c.getPiecesJustificatives().addAll(List.of(pieces));
        return c;
    }

    private PieceJustificative piece(EnumTypeDocument type, EnumStatutVerificationPiece statut) {
        PieceJustificative p = new PieceJustificative();
        p.setTypeDocument(type);
        p.setStatutVerification(statut);
        return p;
    }

    private PieceJustificative piece(EnumTypeDocument type) {
        return piece(type, EnumStatutVerificationPiece.A_VERIFIER);
    }

    @Test
    void le_diplome_est_exige_de_tout_candidat_sauf_en_4e_secondaire() {
        assertThat(CompletudeDossier.piecesRequises(dossier("Licence (L3)")))
                .containsExactly(EnumTypeDocument.PIECE_IDENTITE, EnumTypeDocument.CV,
                        EnumTypeDocument.RELEVE_NOTES, EnumTypeDocument.DIPLOME);
        assertThat(CompletudeDossier.piecesRequises(dossier("4e secondaire")))
                .containsExactly(EnumTypeDocument.PIECE_IDENTITE, EnumTypeDocument.CV, EnumTypeDocument.RELEVE_NOTES);
    }

    /** Un niveau absent ou inconnu n'exonère pas : exempter par erreur coûterait un diplôme manquant. */
    @Test
    void un_niveau_absent_ou_inconnu_n_exonere_pas_du_diplome() {
        assertThat(CompletudeDossier.piecesRequises(dossier(null))).contains(EnumTypeDocument.DIPLOME);
        assertThat(CompletudeDossier.piecesRequises(dossier("Terminale"))).contains(EnumTypeDocument.DIPLOME);
        assertThat(CompletudeDossier.piecesRequises(dossier("4E SECONDAIRE"))).contains(EnumTypeDocument.DIPLOME);
    }

    @Test
    void un_dossier_vide_reclame_toutes_les_pieces() {
        Candidature c = dossier("Master (M2)");

        assertThat(CompletudeDossier.documentsAttendus(c)).extracting(DocumentAttenduDto::etat)
                .containsOnly(Etat.A_FOURNIR);
        assertThat(CompletudeDossier.manques(c)).hasSize(4).allMatch(m -> m.endsWith("(non fournie)"));
    }

    @Test
    void un_dossier_complet_ne_manque_de_rien() {
        Candidature c = dossier("Master (M2)",
                piece(EnumTypeDocument.PIECE_IDENTITE), piece(EnumTypeDocument.CV),
                piece(EnumTypeDocument.RELEVE_NOTES), piece(EnumTypeDocument.DIPLOME));

        assertThat(CompletudeDossier.manques(c)).isEmpty();
        assertThat(CompletudeDossier.documentsAttendus(c)).extracting(DocumentAttenduDto::etat)
                .containsOnly(Etat.FOURNI);
    }

    /** Une pièce encore à contrôler compte comme fournie : le candidat n'a plus rien à faire. */
    @Test
    void une_piece_en_attente_de_controle_ou_conforme_est_fournie() {
        Candidature c = dossier("4e secondaire",
                piece(EnumTypeDocument.PIECE_IDENTITE, EnumStatutVerificationPiece.A_VERIFIER),
                piece(EnumTypeDocument.CV, EnumStatutVerificationPiece.CONFORME),
                piece(EnumTypeDocument.RELEVE_NOTES, EnumStatutVerificationPiece.A_VERIFIER));

        assertThat(CompletudeDossier.manques(c)).isEmpty();
    }

    @Test
    void une_piece_refusee_par_l_admission_est_a_remplacer() {
        Candidature c = dossier("4e secondaire",
                piece(EnumTypeDocument.PIECE_IDENTITE), piece(EnumTypeDocument.CV, EnumStatutVerificationPiece.A_REMPLACER),
                piece(EnumTypeDocument.RELEVE_NOTES));

        assertThat(CompletudeDossier.manques(c)).containsExactly("CV (à remplacer : non conforme)");
        assertThat(CompletudeDossier.documentsAttendus(c))
                .filteredOn(d -> d.type() == EnumTypeDocument.CV).extracting(DocumentAttenduDto::etat)
                .containsExactly(Etat.A_REMPLACER);
    }

    /** Plusieurs relevés déposés dont un seul refusé : le type reste « à remplacer », les autres ne le sauvent pas. */
    @Test
    void un_seul_releve_refuse_parmi_plusieurs_suffit_a_reclamer_le_remplacement() {
        Candidature c = dossier("4e secondaire",
                piece(EnumTypeDocument.PIECE_IDENTITE), piece(EnumTypeDocument.CV),
                piece(EnumTypeDocument.RELEVE_NOTES, EnumStatutVerificationPiece.CONFORME),
                piece(EnumTypeDocument.RELEVE_NOTES, EnumStatutVerificationPiece.A_REMPLACER));

        assertThat(CompletudeDossier.manques(c)).containsExactly("Relevé de notes (à remplacer : non conforme)");
    }

    /** Une pièce facultative refusée se signale quand même : le candidat l'a fournie, elle est inexploitable. */
    @Test
    void une_piece_facultative_refusee_se_signale_aussi() {
        Candidature c = dossier("4e secondaire",
                piece(EnumTypeDocument.PIECE_IDENTITE), piece(EnumTypeDocument.CV), piece(EnumTypeDocument.RELEVE_NOTES),
                piece(EnumTypeDocument.DIPLOME, EnumStatutVerificationPiece.A_REMPLACER));

        assertThat(CompletudeDossier.manques(c)).hasSize(1).first().asString().endsWith("(à remplacer : non conforme)");
        assertThat(CompletudeDossier.documentsAttendus(c)).extracting(DocumentAttenduDto::type)
                .doesNotContain(EnumTypeDocument.DIPLOME);
    }

    @Test
    void la_liste_de_suivi_et_la_relance_disent_la_meme_chose() {
        Candidature c = dossier("Licence (L3)",
                piece(EnumTypeDocument.PIECE_IDENTITE), piece(EnumTypeDocument.CV));

        long aFaireDansLeSuivi = CompletudeDossier.documentsAttendus(c).stream()
                .filter(d -> d.etat() != Etat.FOURNI).count();

        assertThat(CompletudeDossier.manques(c)).hasSize((int) aFaireDansLeSuivi);
    }

    // ===================== Une pièce refusée puis remplacée n'est plus « à remplacer » =====================

    private static final java.time.LocalDateTime T0 = java.time.LocalDateTime.of(2026, 10, 1, 9, 0);

    private PieceJustificative refusee(EnumTypeDocument type, java.time.LocalDateTime deposeLe, java.time.LocalDateTime refuseLe) {
        return DossiersDeTest.piece(type, EnumStatutVerificationPiece.A_REMPLACER, deposeLe, refuseLe);
    }

    private PieceJustificative deposee(EnumTypeDocument type, java.time.LocalDateTime deposeLe) {
        return DossiersDeTest.piece(type, EnumStatutVerificationPiece.A_VERIFIER, deposeLe, null);
    }

    private Candidature dossierBase(PieceJustificative... pieces) {
        return dossier("4e secondaire", pieces);
    }

    /**
     * Le défaut : la pièce refusée reste en base (preuve du refus, motif). La compter telle quelle laissait le dossier
     * « à remplacer » pour toujours, même une fois le remplaçant déposé.
     */
    @Test
    void une_piece_refusee_puis_remplacee_par_un_nouveau_depot_n_est_plus_a_remplacer() {
        Candidature c = dossierBase(piece(EnumTypeDocument.PIECE_IDENTITE), piece(EnumTypeDocument.RELEVE_NOTES),
                refusee(EnumTypeDocument.CV, T0, T0.plusDays(1)),
                deposee(EnumTypeDocument.CV, T0.plusDays(2)));

        assertThat(CompletudeDossier.manques(c)).isEmpty();
        assertThat(CompletudeDossier.documentsAttendus(c)).extracting(DocumentAttenduDto::etat).containsOnly(Etat.FOURNI);
    }

    @Test
    void un_depot_anterieur_au_refus_ne_remplace_rien() {
        Candidature c = dossierBase(piece(EnumTypeDocument.PIECE_IDENTITE), piece(EnumTypeDocument.RELEVE_NOTES),
                deposee(EnumTypeDocument.CV, T0),
                refusee(EnumTypeDocument.CV, T0.plusDays(1), T0.plusDays(3)));

        assertThat(CompletudeDossier.manques(c)).containsExactly("CV (à remplacer : non conforme)");
    }

    @Test
    void une_piece_refusee_sans_nouveau_depot_reste_a_remplacer() {
        Candidature c = dossierBase(piece(EnumTypeDocument.PIECE_IDENTITE), piece(EnumTypeDocument.RELEVE_NOTES),
                refusee(EnumTypeDocument.CV, T0, T0.plusDays(1)));

        assertThat(CompletudeDossier.manques(c)).containsExactly("CV (à remplacer : non conforme)");
    }

    /** Un remplaçant ne couvre qu'un refus : trois relevés refusés réclament trois dépôts. */
    @Test
    void chaque_remplacant_ne_couvre_qu_un_seul_refus() {
        Candidature c = dossierBase(piece(EnumTypeDocument.PIECE_IDENTITE), piece(EnumTypeDocument.CV),
                refusee(EnumTypeDocument.RELEVE_NOTES, T0, T0.plusDays(1)),
                refusee(EnumTypeDocument.RELEVE_NOTES, T0, T0.plusDays(1)),
                deposee(EnumTypeDocument.RELEVE_NOTES, T0.plusDays(2)));

        assertThat(CompletudeDossier.manques(c)).containsExactly("Relevé de notes (à remplacer : non conforme)");

        c.getPiecesJustificatives().add(deposee(EnumTypeDocument.RELEVE_NOTES, T0.plusDays(3)));
        assertThat(CompletudeDossier.manques(c)).isEmpty();
    }

    @Test
    void un_remplacant_a_son_tour_refuse_ne_remplace_rien() {
        Candidature c = dossierBase(piece(EnumTypeDocument.PIECE_IDENTITE), piece(EnumTypeDocument.RELEVE_NOTES),
                refusee(EnumTypeDocument.CV, T0, T0.plusDays(1)),
                refusee(EnumTypeDocument.CV, T0.plusDays(2), T0.plusDays(3)));

        assertThat(CompletudeDossier.manques(c)).hasSize(1);
    }

    /** Sans date de refus ni de dépôt on ne peut pas prouver un remplacement : on ne l'affirme pas. */
    @Test
    void sans_dates_on_n_affirme_pas_un_remplacement() {
        Candidature c = dossierBase(piece(EnumTypeDocument.PIECE_IDENTITE), piece(EnumTypeDocument.RELEVE_NOTES),
                piece(EnumTypeDocument.CV, EnumStatutVerificationPiece.A_REMPLACER),
                piece(EnumTypeDocument.CV));

        assertThat(CompletudeDossier.manques(c)).containsExactly("CV (à remplacer : non conforme)");
    }
}
