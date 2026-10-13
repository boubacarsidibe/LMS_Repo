package inpg.sn.lms.recrutement.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import inpg.sn.lms.auth.entity.Candidat;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.recrutement.entity.Candidature;
import inpg.sn.lms.recrutement.entity.PieceJustificative;
import inpg.sn.lms.recrutement.enums.EnumStatutCandidature;
import inpg.sn.lms.recrutement.enums.EnumStatutVerificationPiece;
import inpg.sn.lms.recrutement.enums.EnumTypeDocument;
import inpg.sn.lms.recrutement.repository.CandidatureRepository;
import inpg.sn.lms.recrutement.service.NotificationCandidatService;
import inpg.sn.lms.recrutement.service.NotificationService;

/**
 * Relance des dossiers incomplets (TDR : « aux candidats n'ayant pas un dossier complet,
 * de pouvoir revenir plus tard » — encore fallait-il les prévenir). L'endpoint ne faisait
 * jusqu'ici que compter les dossiers concernés sans jamais rien envoyer.
 */
@ExtendWith(MockitoExtension.class)
class ApplicationControllerTest {

    @Mock private CandidatureRepository candidatureRepo;
    @Mock private NotificationService notificationService;
    @Mock private NotificationCandidatService notificationCandidatService;

    @InjectMocks private ApplicationController controller;

    private Candidature candidature(long id, String email, List<PieceJustificative> pieces) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        Candidat candidat = new Candidat();
        candidat.setId(id);
        candidat.setUser(user);

        Candidature c = new Candidature();
        c.setId(id);
        c.setNumeroDossier("INPG-TEST" + id);
        c.setCandidat(candidat);
        c.setPiecesJustificatives(pieces);
        // Seuls les dossiers encore en cours d'instruction sont relançables.
        c.setStatut(EnumStatutCandidature.SOUMIS);
        return c;
    }

    /** Les quatre pièces exigées d'un candidat (identité, CV, relevé, diplôme), toutes conformes : un dossier complet. */
    private List<PieceJustificative> dossierComplet() {
        return List.of(
                piece(EnumTypeDocument.PIECE_IDENTITE, true),
                piece(EnumTypeDocument.CV, true),
                piece(EnumTypeDocument.RELEVE_NOTES, true),
                piece(EnumTypeDocument.DIPLOME, true));
    }

    /** Dossier d'un candidat dont le niveau d'études déclaré est {@code niveau} (null : non renseigné). */
    private Candidature candidatureDeNiveau(String niveau, List<PieceJustificative> pieces) {
        Candidature c = candidature(1L, "a@example.sn", pieces);
        c.getCandidat().setNiveauEtude(niveau);
        return c;
    }

    private PieceJustificative piece(EnumTypeDocument type, boolean conforme) {
        PieceJustificative p = new PieceJustificative();
        p.setTypeDocument(type);
        p.setEstConforme(conforme);
        p.setStatutVerification(conforme
                ? EnumStatutVerificationPiece.CONFORME
                : EnumStatutVerificationPiece.A_REMPLACER);
        return p;
    }

    @Test
    void une_piece_en_attente_de_verification_ne_declenche_pas_de_relance_candidat() {
        PieceJustificative identite = piece(EnumTypeDocument.PIECE_IDENTITE, false);
        identite.setStatutVerification(EnumStatutVerificationPiece.A_VERIFIER);
        PieceJustificative cv = piece(EnumTypeDocument.CV, false);
        cv.setStatutVerification(EnumStatutVerificationPiece.A_VERIFIER);
        PieceJustificative releve = piece(EnumTypeDocument.RELEVE_NOTES, false);
        releve.setStatutVerification(EnumStatutVerificationPiece.A_VERIFIER);
        PieceJustificative diplome = piece(EnumTypeDocument.DIPLOME, false);
        diplome.setStatutVerification(EnumStatutVerificationPiece.A_VERIFIER);
        when(candidatureRepo.findAll()).thenReturn(List.of(
                candidature(1L, "a@example.sn", List.of(identite, cv, releve, diplome))));

        ResponseEntity<Map<String, Object>> reponse = controller.sendReminder();

        assertThat(reponse.getBody().get("envoyes")).isEqualTo(0);
        verify(notificationService, never()).notifierRappelDossierIncomplet(anyString(), anyString(), anyString());
    }

    /** Un dossier mis « à compléter » par l'admission est celui qu'il faut relancer en premier. */
    @Test
    void un_dossier_a_completer_est_relance() {
        Candidature enAttente = candidature(1L, "a@example.sn", List.of(piece(EnumTypeDocument.CV, true)));
        enAttente.setStatut(EnumStatutCandidature.INCOMPLET);
        when(candidatureRepo.findAll()).thenReturn(List.of(enAttente));

        ResponseEntity<Map<String, Object>> reponse = controller.sendReminder();

        assertThat(reponse.getBody().get("envoyes")).isEqualTo(1);
        verify(notificationService).notifierRappelDossierIncomplet(anyString(), anyString(), anyString());
    }

    @Test
    void un_dossier_complet_nest_pas_relance() {
        when(candidatureRepo.findAll()).thenReturn(List.of(
                candidature(1L, "a@example.sn", dossierComplet())));

        ResponseEntity<Map<String, Object>> reponse = controller.sendReminder();

        assertThat(reponse.getBody().get("envoyes")).isEqualTo(0);
        verify(notificationService, never()).notifierRappelDossierIncomplet(anyString(), anyString(), anyString());
    }

    @Test
    void un_dossier_sans_aucune_piece_reclame_les_quatre_pieces_requises() {
        when(candidatureRepo.findAll()).thenReturn(List.of(candidature(1L, "a@example.sn", List.of())));

        ResponseEntity<Map<String, Object>> reponse = controller.sendReminder();

        assertThat(reponse.getBody().get("envoyes")).isEqualTo(1);
        verify(notificationService).notifierRappelDossierIncomplet("a@example.sn", "INPG-TEST1",
                "Pièce d'identité (non fournie), CV (non fournie), Relevé de notes (non fournie), "
                        + "Diplôme (non fournie)");
        verify(notificationCandidatService).creerNotification(anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    void une_piece_requise_absente_est_nommee_meme_si_les_autres_sont_conformes() {
        when(candidatureRepo.findAll()).thenReturn(List.of(candidature(1L, "a@example.sn", List.of(
                piece(EnumTypeDocument.CV, true),
                piece(EnumTypeDocument.RELEVE_NOTES, true),
                piece(EnumTypeDocument.DIPLOME, true)))));

        controller.sendReminder();

        // Le CV conforme suffisait auparavant à faire passer le dossier pour complet : le
        // candidat n'était jamais prévenu qu'il manquait sa pièce d'identité.
        verify(notificationService).notifierRappelDossierIncomplet("a@example.sn", "INPG-TEST1",
                "Pièce d'identité (non fournie)");
    }

    @Test
    void une_piece_requise_non_conforme_est_a_remplacer() {
        when(candidatureRepo.findAll()).thenReturn(List.of(candidature(1L, "a@example.sn", List.of(
                piece(EnumTypeDocument.PIECE_IDENTITE, true),
                piece(EnumTypeDocument.CV, false),
                piece(EnumTypeDocument.RELEVE_NOTES, true),
                piece(EnumTypeDocument.DIPLOME, true)))));

        controller.sendReminder();

        verify(notificationService).notifierRappelDossierIncomplet("a@example.sn", "INPG-TEST1",
                "CV (à remplacer : non conforme)");
    }

    @Test
    void une_piece_facultative_refusee_est_signalee_aussi() {
        List<PieceJustificative> pieces = new java.util.ArrayList<>(dossierComplet());
        pieces.add(piece(EnumTypeDocument.LETTRE_MOTIVATION, false));
        when(candidatureRepo.findAll()).thenReturn(List.of(candidature(1L, "a@example.sn", pieces)));

        controller.sendReminder();

        verify(notificationService).notifierRappelDossierIncomplet("a@example.sn", "INPG-TEST1",
                "Lettre de motivation (à remplacer : non conforme)");
    }

    // ---- Attestation de diplôme : exigée par l'INPG, sauf des candidats de niveau 4e secondaire (CPS) ----

    private List<PieceJustificative> sansDiplome() {
        return List.of(
                piece(EnumTypeDocument.PIECE_IDENTITE, true),
                piece(EnumTypeDocument.CV, true),
                piece(EnumTypeDocument.RELEVE_NOTES, true));
    }

    @Test
    void le_diplome_manquant_est_reclame_a_un_candidat_de_licence() {
        when(candidatureRepo.findAll()).thenReturn(List.of(candidatureDeNiveau("Licence (L3)", sansDiplome())));

        controller.sendReminder();

        verify(notificationService).notifierRappelDossierIncomplet("a@example.sn", "INPG-TEST1",
                "Diplôme (non fournie)");
    }

    @Test
    void un_candidat_de_niveau_4e_secondaire_n_a_pas_de_diplome_a_fournir() {
        when(candidatureRepo.findAll()).thenReturn(List.of(candidatureDeNiveau("4e secondaire", sansDiplome())));

        ResponseEntity<Map<String, Object>> reponse = controller.sendReminder();

        assertThat(reponse.getBody().get("envoyes")).isEqualTo(0);
        verify(notificationService, never()).notifierRappelDossierIncomplet(anyString(), anyString(), anyString());
    }

    @Test
    void un_niveau_non_renseigne_n_exonere_pas_du_diplome() {
        when(candidatureRepo.findAll()).thenReturn(List.of(candidatureDeNiveau(null, sansDiplome())));

        controller.sendReminder();

        verify(notificationService).notifierRappelDossierIncomplet("a@example.sn", "INPG-TEST1",
                "Diplôme (non fournie)");
    }

    @Test
    void un_diplome_depose_par_un_candidat_de_4e_secondaire_reste_controle() {
        List<PieceJustificative> pieces = new java.util.ArrayList<>(sansDiplome());
        pieces.add(piece(EnumTypeDocument.DIPLOME, false));
        when(candidatureRepo.findAll()).thenReturn(List.of(candidatureDeNiveau("4e secondaire", pieces)));

        controller.sendReminder();

        verify(notificationService).notifierRappelDossierIncomplet("a@example.sn", "INPG-TEST1",
                "Diplôme (à remplacer : non conforme)");
    }

    @Test
    void un_dossier_deja_admis_nest_jamais_relance() {
        Candidature admis = candidature(1L, "a@example.sn", List.of());
        admis.setStatut(EnumStatutCandidature.ADMIS);
        when(candidatureRepo.findAll()).thenReturn(List.of(admis));

        ResponseEntity<Map<String, Object>> reponse = controller.sendReminder();

        assertThat(reponse.getBody().get("envoyes")).isEqualTo(0);
        verify(notificationService, never()).notifierRappelDossierIncomplet(anyString(), anyString(), anyString());
    }

    @Test
    void un_candidat_sans_adresse_email_est_compte_en_echec_pas_en_exception() {
        when(candidatureRepo.findAll()).thenReturn(List.of(candidature(1L, null, List.of())));

        ResponseEntity<Map<String, Object>> reponse = controller.sendReminder();

        assertThat(reponse.getBody().get("envoyes")).isEqualTo(0);
        @SuppressWarnings("unchecked")
        List<String> echecs = (List<String>) reponse.getBody().get("echecs");
        assertThat(echecs).hasSize(1);
        verify(notificationService, never()).notifierRappelDossierIncomplet(anyString(), anyString(), anyString());
    }

    @Test
    void plusieurs_dossiers_incomplets_sont_tous_relances() {
        when(candidatureRepo.findAll()).thenReturn(List.of(
                candidature(1L, "a@example.sn", List.of()),
                candidature(2L, "b@example.sn", List.of(piece(EnumTypeDocument.CV, false))),
                candidature(3L, "c@example.sn", dossierComplet())));

        ResponseEntity<Map<String, Object>> reponse = controller.sendReminder();

        assertThat(reponse.getBody().get("envoyes")).isEqualTo(2);
        verify(notificationService, times(2))
                .notifierRappelDossierIncomplet(anyString(), anyString(), anyString());
    }
}
