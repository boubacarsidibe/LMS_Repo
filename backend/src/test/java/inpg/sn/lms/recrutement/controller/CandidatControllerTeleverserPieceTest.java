package inpg.sn.lms.recrutement.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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

    /**
     * Un dossier qui vient d'être déposé (SOUMIS) doit recevoir ses pièces : c'est ce que fait le formulaire juste après
     * avoir créé la candidature. Les tests de cette classe passaient tous par SOUMIS et échouaient tous depuis que la garde
     * n'acceptait plus que PRESELECTIONNE et PAIEMENT_EN_ATTENTE.
     */
    @ParameterizedTest
    @EnumSource(value = EnumStatutCandidature.class, names = {
        "BROUILLON", "SOUMIS", "INCOMPLET", "DOSSIER_VALIDE", "PRESELECTIONNE", "PAIEMENT_EN_ATTENTE", "PAIEMENT_CONFIRME" })
    void les_pieces_sont_acceptees_tant_que_le_candidat_n_est_pas_convoque_au_test(EnumStatutCandidature statut) {
        candidature.setStatut(statut);

        ResponseEntity<?> reponse = controller.televerserPiece(
                ID_CANDIDATURE, fichier, "CV", null, null, authentication);

        assertThat(reponse.getStatusCode()).as(statut.name()).isEqualTo(HttpStatus.OK);
    }

    @ParameterizedTest
    @EnumSource(value = EnumStatutCandidature.class, names = {
        "DOSSIER_REJETE", "CONVOQUE_TEST", "TEST_REUSSI", "TEST_ECHOUE", "ABSENT_TEST", "CONVOQUE_ENTRETIEN",
        "ENTRETIEN_EFFECTUE", "ABSENT_ENTRETIEN", "ADMIS", "LISTE_ATTENTE", "NON_ADMIS", "INSCRIPTION_FINALISEE",
        "DESISTEMENT" })
    void une_piece_est_figee_des_que_le_jury_a_convoque_ou_decide(EnumStatutCandidature statut) {
        candidature.setStatut(statut);

        ResponseEntity<?> reponse = controller.televerserPiece(
                ID_CANDIDATURE, fichier, "CV", null, null, authentication);

        assertThat(reponse.getStatusCode()).as(statut.name()).isEqualTo(HttpStatus.CONFLICT);
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
}
