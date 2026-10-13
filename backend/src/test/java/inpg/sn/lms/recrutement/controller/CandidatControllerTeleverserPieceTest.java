package inpg.sn.lms.recrutement.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;

import com.fasterxml.jackson.databind.ObjectMapper;

import inpg.sn.lms.auth.entity.Candidat;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.CandidatRepository;
import inpg.sn.lms.auth.repository.RoleRepository;
import inpg.sn.lms.auth.repository.UserRepository;
import inpg.sn.lms.recrutement.entity.Candidature;
import inpg.sn.lms.recrutement.entity.FichierStocke;
import inpg.sn.lms.recrutement.entity.PieceJustificative;
import inpg.sn.lms.recrutement.enums.EnumTypeDocument;
import inpg.sn.lms.recrutement.enums.EnumStatutCandidature;
import inpg.sn.lms.recrutement.repository.BrouillonCandidatureRepository;
import inpg.sn.lms.recrutement.repository.CandidatureRepository;
import inpg.sn.lms.recrutement.repository.CritereAdmissionPersonnaliseRepository;
import inpg.sn.lms.recrutement.repository.EntretienRepository;
import inpg.sn.lms.recrutement.repository.PieceJustificativeRepository;
import inpg.sn.lms.recrutement.repository.SessionCandidatureRepository;
import inpg.sn.lms.recrutement.repository.TestAcademiqueRepository;
import inpg.sn.lms.recrutement.service.CandidatureService;
import inpg.sn.lms.recrutement.service.ConvocationPdfService;
import inpg.sn.lms.recrutement.service.EntretienService;
import inpg.sn.lms.recrutement.service.FichierStockeService;
import inpg.sn.lms.recrutement.service.LettreAdmissionPdfService;
import inpg.sn.lms.recrutement.service.NotificationCandidatService;
import inpg.sn.lms.recrutement.service.NotificationService;
import inpg.sn.lms.recrutement.service.PaiementService;

/**
 * Un diplôme/certificat s'ajoute en nombre libre depuis le formulaire de candidature
 * (bouton « + », à l'image d'une liste de compétences UNESCO) : rien d'autre que la
 * description saisie par le candidat ne permet ensuite de savoir à quoi il correspond.
 * Ce test fige la règle serveur qui la rend obligatoire pour ce type de document —
 * sans quoi un client autre que le formulaire pourrait déposer un diplôme muet.
 */
@ExtendWith(MockitoExtension.class)
class CandidatControllerTeleverserPieceTest {

    private static final long ID_UTILISATEUR = 1L;
    private static final long ID_CANDIDATURE = 42L;

    @Mock private CandidatureService candidatureService;
    @Mock private CandidatRepository candidatRepository;
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private SessionCandidatureRepository sessionRepository;
    @Mock private CandidatureRepository candidatureRepository;
    @Mock private FichierStockeService fichierStockeService;
    @Mock private PieceJustificativeRepository pieceJustificativeRepository;
    @Mock private BrouillonCandidatureRepository brouillonRepository;
    @Mock private NotificationService notificationService;
    @Mock private NotificationCandidatService notificationCandidatService;
    @Mock private PaiementService paiementService;
    @Mock private TestAcademiqueRepository testAcademiqueRepository;
    @Mock private EntretienRepository entretienRepository;
    @Mock private EntretienService entretienService;
    @Mock private ConvocationPdfService convocationPdfService;
    @Mock private LettreAdmissionPdfService lettreAdmissionPdfService;
    @Mock private CritereAdmissionPersonnaliseRepository critereRepository;
    @Mock private ObjectMapper objectMapper;

    @Mock private Authentication authentication;

    @InjectMocks private CandidatController controller;

    private Candidat candidat;
    private Candidature candidature;
    private MockMultipartFile fichier;

    @BeforeEach
    void preparerDossierExistant() {
        User utilisateur = new User();
        utilisateur.setId(ID_UTILISATEUR);
        utilisateur.setUsername("awa_diop");

        candidat = new Candidat();
        candidat.setId(ID_UTILISATEUR);

        candidature = new Candidature();
        candidature.setId(ID_CANDIDATURE);
        candidature.setStatut(EnumStatutCandidature.SOUMIS);
        candidature.setCandidat(candidat);

        fichier = new MockMultipartFile("file", "diplome.pdf", "application/pdf", new byte[] { 1, 2, 3 });

        lenient().when(authentication.getName()).thenReturn("awa_diop");
        lenient().when(userRepository.findByUsername("awa_diop")).thenReturn(utilisateur);
        lenient().when(candidatRepository.findByUserId(ID_UTILISATEUR)).thenReturn(java.util.Optional.of(candidat));
        lenient().when(candidatureRepository.findById(ID_CANDIDATURE)).thenReturn(java.util.Optional.of(candidature));

        FichierStocke stocke = new FichierStocke();
        stocke.setId(99L);
        stocke.setNomOriginal("diplome.pdf");
        lenient().when(fichierStockeService.enregistrerFichier(any())).thenReturn(stocke);
    }

    /** Dossier à compléter : l'admission attend justement une pièce, la refuser serait une impasse. */
    @Test
    void un_dossier_incomplet_accepte_une_piece() {
        candidature.setStatut(EnumStatutCandidature.INCOMPLET);

        ResponseEntity<?> reponse = controller.televerserPiece(
                ID_CANDIDATURE, fichier, "CV", null, null, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    /** Une fois le dossier validé, présélectionné ou plus loin, les pièces sont figées. */
    @Test
    void un_dossier_apres_la_decision_est_fige() {
        for (EnumStatutCandidature statut : new EnumStatutCandidature[] {
                EnumStatutCandidature.DOSSIER_VALIDE, EnumStatutCandidature.PRESELECTIONNE,
                EnumStatutCandidature.PAIEMENT_EN_ATTENTE, EnumStatutCandidature.CONVOQUE_TEST,
                EnumStatutCandidature.ADMIS, EnumStatutCandidature.DOSSIER_REJETE }) {
            candidature.setStatut(statut);

            ResponseEntity<?> reponse = controller.televerserPiece(
                    ID_CANDIDATURE, fichier, "CV", null, null, authentication);

            assertThat(reponse.getStatusCode()).as("statut " + statut).isEqualTo(HttpStatus.CONFLICT);
        }
        verify(pieceJustificativeRepository, never()).save(any());
    }

    @Test
    void un_diplome_sans_description_est_refuse() {
        ResponseEntity<?> reponse = controller.televerserPiece(
                ID_CANDIDATURE, fichier, "DIPLOME", null, null, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(pieceJustificativeRepository, never()).save(any());
    }

    @Test
    void un_diplome_avec_description_uniquement_faite_d_espaces_est_refuse() {
        ResponseEntity<?> reponse = controller.televerserPiece(
                ID_CANDIDATURE, fichier, "DIPLOME", "   ", null, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(pieceJustificativeRepository, never()).save(any());
    }

    @Test
    void un_diplome_avec_description_est_accepte_et_la_description_est_enregistree() {
        ResponseEntity<?> reponse = controller.televerserPiece(
                ID_CANDIDATURE, fichier, "DIPLOME", "Licence en Informatique, 2022", null, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        ArgumentCaptor<PieceJustificative> captor = ArgumentCaptor.forClass(PieceJustificative.class);
        verify(pieceJustificativeRepository).save(captor.capture());
        assertThat(captor.getValue().getDescription()).isEqualTo("Licence en Informatique, 2022");
    }

    @Test
    void un_cv_sans_description_reste_accepte() {
        ResponseEntity<?> reponse = controller.televerserPiece(
                ID_CANDIDATURE, fichier, "CV", null, null, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        ArgumentCaptor<PieceJustificative> captor = ArgumentCaptor.forClass(PieceJustificative.class);
        verify(pieceJustificativeRepository).save(captor.capture());
        assertThat(captor.getValue().getDescription()).isNull();
    }

    @Test
    void une_description_trop_longue_est_refusee() {
        String tropLongue = "x".repeat(501);

        ResponseEntity<?> reponse = controller.televerserPiece(
                ID_CANDIDATURE, fichier, "CV", tropLongue, null, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(pieceJustificativeRepository, never()).save(any());
    }

    // ===================== Renvoi d'une pièce déjà reçue =====================

    /**
     * Le formulaire relance un envoi qui a échoué sans réponse (coupure, délai dépassé). Si le premier essai avait
     * en réalité abouti, le second créait une seconde pièce : l'admission voyait deux CV identiques.
     */
    @Test
    void le_renvoi_a_l_identique_d_une_piece_deja_recue_n_en_cree_pas_une_seconde(@TempDir Path dossier) throws Exception {
        byte[] contenu = { 1, 2, 3 };
        PieceJustificative dejaLa = pieceEnregistree(dossier, "cv.pdf", contenu, EnumTypeDocument.CV, null, null);
        when(pieceJustificativeRepository.findByCandidatureIdAndTypeDocument(ID_CANDIDATURE, EnumTypeDocument.CV))
                .thenReturn(List.of(dejaLa));
        MockMultipartFile renvoi = new MockMultipartFile("file", "cv.pdf", "application/pdf", contenu);

        ResponseEntity<?> reponse = controller.televerserPiece(ID_CANDIDATURE, renvoi, "CV", null, null, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Map<?, ?>) reponse.getBody()).get("doublon")).isEqualTo("true");
        assertThat(((Map<?, ?>) reponse.getBody()).get("fichierId")).isEqualTo("77");
        verify(fichierStockeService, never()).enregistrerFichier(any());
        verify(pieceJustificativeRepository, never()).save(any());
    }

    /** Deux relevés distincts qui porteraient le même nom restent deux pièces : on compare le contenu, pas le nom. */
    @Test
    void un_autre_contenu_sous_le_meme_nom_est_une_nouvelle_piece(@TempDir Path dossier) throws Exception {
        PieceJustificative dejaLa = pieceEnregistree(dossier, "cv.pdf", new byte[] { 1, 2, 3 }, EnumTypeDocument.CV, null, null);
        when(pieceJustificativeRepository.findByCandidatureIdAndTypeDocument(ID_CANDIDATURE, EnumTypeDocument.CV))
                .thenReturn(List.of(dejaLa));
        MockMultipartFile autre = new MockMultipartFile("file", "cv.pdf", "application/pdf", new byte[] { 9, 9, 9 });

        ResponseEntity<?> reponse = controller.televerserPiece(ID_CANDIDATURE, autre, "CV", null, null, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Map<?, ?>) reponse.getBody()).get("doublon")).isNull();
        verify(pieceJustificativeRepository).save(any());
    }

    @Test
    void la_meme_description_est_exigee_pour_reconnaitre_un_doublon(@TempDir Path dossier) throws Exception {
        byte[] contenu = { 1, 2, 3 };
        PieceJustificative dejaLa = pieceEnregistree(dossier, "diplome.pdf", contenu, EnumTypeDocument.DIPLOME, "Licence 2022", null);
        when(pieceJustificativeRepository.findByCandidatureIdAndTypeDocument(ID_CANDIDATURE, EnumTypeDocument.DIPLOME))
                .thenReturn(List.of(dejaLa));

        ResponseEntity<?> autreDiplome = controller.televerserPiece(ID_CANDIDATURE,
                new MockMultipartFile("file", "diplome.pdf", "application/pdf", contenu), "DIPLOME", "Master 2024", null, authentication);

        assertThat(((Map<?, ?>) autreDiplome.getBody()).get("doublon")).isNull();
        verify(pieceJustificativeRepository).save(any());
    }

    @Test
    void la_meme_moyenne_declaree_est_exigee_pour_reconnaitre_un_releve(@TempDir Path dossier) throws Exception {
        byte[] contenu = { 1, 2, 3 };
        PieceJustificative dejaLa = pieceEnregistree(dossier, "releve.pdf", contenu, EnumTypeDocument.RELEVE_NOTES, null, 12.5);
        when(pieceJustificativeRepository.findByCandidatureIdAndTypeDocument(ID_CANDIDATURE, EnumTypeDocument.RELEVE_NOTES))
                .thenReturn(List.of(dejaLa));
        MockMultipartFile releve = new MockMultipartFile("file", "releve.pdf", "application/pdf", contenu);

        ResponseEntity<?> identique = controller.televerserPiece(ID_CANDIDATURE, releve, "RELEVE_NOTES", null, 12.5, authentication);
        assertThat(((Map<?, ?>) identique.getBody()).get("doublon")).isEqualTo("true");

        ResponseEntity<?> corrigee = controller.televerserPiece(ID_CANDIDATURE, releve, "RELEVE_NOTES", null, 14.0, authentication);
        assertThat(((Map<?, ?>) corrigee.getBody()).get("doublon")).isNull();
    }

    /** Un fichier disparu du disque n'est jamais « le même » : le renvoi le rétablit au lieu de l'ignorer. */
    @Test
    void une_piece_dont_le_fichier_a_disparu_du_disque_est_renvoyee_pour_de_bon(@TempDir Path dossier) throws Exception {
        byte[] contenu = { 1, 2, 3 };
        PieceJustificative dejaLa = pieceEnregistree(dossier, "cv.pdf", contenu, EnumTypeDocument.CV, null, null);
        Files.delete(Path.of(dejaLa.getFichierStocke().getCheminStockage()));
        when(pieceJustificativeRepository.findByCandidatureIdAndTypeDocument(ID_CANDIDATURE, EnumTypeDocument.CV))
                .thenReturn(List.of(dejaLa));

        ResponseEntity<?> reponse = controller.televerserPiece(ID_CANDIDATURE,
                new MockMultipartFile("file", "cv.pdf", "application/pdf", contenu), "CV", null, null, authentication);

        assertThat(((Map<?, ?>) reponse.getBody()).get("doublon")).isNull();
        verify(fichierStockeService).enregistrerFichier(any());
        verify(pieceJustificativeRepository).save(any());
    }

    /** Le neuvième relevé, déjà reçu, doit être reconnu — et non refusé comme s'il était un dixième. */
    @Test
    void le_renvoi_du_neuvieme_releve_n_est_pas_refuse_comme_un_dixieme(@TempDir Path dossier) throws Exception {
        byte[] contenu = { 1, 2, 3 };
        PieceJustificative dejaLa = pieceEnregistree(dossier, "releve9.pdf", contenu, EnumTypeDocument.RELEVE_NOTES, null, 13.0);
        lenient().when(pieceJustificativeRepository.countByCandidatureIdAndTypeDocument(ID_CANDIDATURE, EnumTypeDocument.RELEVE_NOTES))
                .thenReturn(9L);
        when(pieceJustificativeRepository.findByCandidatureIdAndTypeDocument(ID_CANDIDATURE, EnumTypeDocument.RELEVE_NOTES))
                .thenReturn(List.of(dejaLa));

        ResponseEntity<?> reponse = controller.televerserPiece(ID_CANDIDATURE,
                new MockMultipartFile("file", "releve9.pdf", "application/pdf", contenu), "RELEVE_NOTES", null, 13.0, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Map<?, ?>) reponse.getBody()).get("doublon")).isEqualTo("true");
    }

    @Test
    void un_dixieme_releve_different_reste_refuse() {
        when(pieceJustificativeRepository.findByCandidatureIdAndTypeDocument(ID_CANDIDATURE, EnumTypeDocument.RELEVE_NOTES))
                .thenReturn(List.of());
        when(pieceJustificativeRepository.countByCandidatureIdAndTypeDocument(ID_CANDIDATURE, EnumTypeDocument.RELEVE_NOTES))
                .thenReturn(9L);

        ResponseEntity<?> reponse = controller.televerserPiece(ID_CANDIDATURE, fichier, "RELEVE_NOTES", null, 11.0, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(pieceJustificativeRepository, never()).save(any());
    }

    // ===================== Retour automatique à l'examen d'un dossier « à compléter » =====================

    private PieceJustificative dejaDeposee(EnumTypeDocument type) {
        PieceJustificative piece = new PieceJustificative();
        piece.setTypeDocument(type);
        piece.setStatutVerification(inpg.sn.lms.recrutement.enums.EnumStatutVerificationPiece.A_VERIFIER);
        candidature.getPiecesJustificatives().add(piece);
        return piece;
    }

    /**
     * La page de suivi promet « votre dossier sera de nouveau examiné » à un candidat dont l'admission a demandé un
     * complément. Rien ne ramenait pourtant le dossier à l'examen : il restait « à compléter » une fois complété.
     */
    @Test
    void la_derniere_piece_manquante_ramene_le_dossier_a_l_examen() {
        candidature.setStatut(EnumStatutCandidature.INCOMPLET);
        candidat.setNiveauEtude("4e secondaire");   // pas de diplôme exigé : identité, CV, relevé
        dejaDeposee(EnumTypeDocument.PIECE_IDENTITE);
        dejaDeposee(EnumTypeDocument.RELEVE_NOTES);

        ResponseEntity<?> reponse = controller.televerserPiece(ID_CANDIDATURE, fichier, "CV", null, null, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(candidature.getStatut()).isEqualTo(EnumStatutCandidature.SOUMIS);
        verify(candidatureRepository).save(candidature);
        verify(candidatureService).journaliser(eq(ID_CANDIDATURE), eq(inpg.sn.lms.recrutement.enums.EnumEtapeQms.RECEPTION),
                eq(EnumStatutCandidature.SOUMIS), any(), org.mockito.ArgumentMatchers.contains("Complément reçu"));
    }

    @Test
    void une_piece_qui_ne_complete_pas_le_dossier_le_laisse_a_completer() {
        candidature.setStatut(EnumStatutCandidature.INCOMPLET);
        candidat.setNiveauEtude("4e secondaire");
        dejaDeposee(EnumTypeDocument.PIECE_IDENTITE);   // il manque encore le CV et le relevé

        controller.televerserPiece(ID_CANDIDATURE, fichier, "RELEVE_NOTES", null, 12.0, authentication);

        assertThat(candidature.getStatut()).isEqualTo(EnumStatutCandidature.INCOMPLET);
        verify(candidatureService, never()).journaliser(any(), any(), any(), any(), any());
    }

    @Test
    void un_dossier_soumis_qui_se_complete_reste_soumis_sans_ecriture_de_journal() {
        candidature.setStatut(EnumStatutCandidature.SOUMIS);
        candidat.setNiveauEtude("4e secondaire");
        dejaDeposee(EnumTypeDocument.PIECE_IDENTITE);
        dejaDeposee(EnumTypeDocument.RELEVE_NOTES);

        controller.televerserPiece(ID_CANDIDATURE, fichier, "CV", null, null, authentication);

        assertThat(candidature.getStatut()).isEqualTo(EnumStatutCandidature.SOUMIS);
        verify(candidatureService, never()).journaliser(any(), any(), any(), any(), any());
    }

    /** Écrit réellement le fichier sur disque : la reconnaissance d'un doublon compare les octets, pas des métadonnées. */
    private PieceJustificative pieceEnregistree(Path dossier, String nom, byte[] contenu, EnumTypeDocument type,
            String description, Double moyenne) throws Exception {
        Path chemin = dossier.resolve("77_" + nom);
        Files.write(chemin, contenu);
        FichierStocke stocke = new FichierStocke();
        stocke.setId(77L);
        stocke.setNomOriginal(nom);
        stocke.setCheminStockage(chemin.toString());
        stocke.setTailleOctets((long) contenu.length);
        PieceJustificative piece = new PieceJustificative();
        piece.setTypeDocument(type);
        piece.setDescription(description);
        piece.setMoyenneDeclaree(moyenne);
        piece.setFichierStocke(stocke);
        piece.setCandidature(candidature);
        return piece;
    }
}
