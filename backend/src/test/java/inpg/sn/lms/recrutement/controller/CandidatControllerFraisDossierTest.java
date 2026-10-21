package inpg.sn.lms.recrutement.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import com.fasterxml.jackson.databind.ObjectMapper;

import inpg.sn.lms.auth.entity.Candidat;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.CandidatRepository;
import inpg.sn.lms.auth.repository.RoleRepository;
import inpg.sn.lms.auth.repository.UserRepository;
import inpg.sn.lms.recrutement.entity.Candidature;
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
 * Le candidat doit connaître le montant avant de déclarer son règlement. L'endpoint qui l'annonce est réservé au
 * titulaire du dossier : le tarif d'une session n'a rien de secret, mais le lien dossier ↔ session, lui, en est un.
 */
@ExtendWith(MockitoExtension.class)
class CandidatControllerFraisDossierTest {

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

    private Candidature candidature;

    @BeforeEach
    void preparer() {
        User utilisateur = new User();
        utilisateur.setId(1L);
        Candidat moi = new Candidat();
        moi.setId(1L);
        Candidat autre = new Candidat();
        autre.setId(2L);

        candidature = new Candidature();
        candidature.setId(42L);
        candidature.setCandidat(autre);

        when(authentication.getName()).thenReturn("awa_diop");
        when(userRepository.findByUsername("awa_diop")).thenReturn(utilisateur);
        when(candidatRepository.findByUserId(1L)).thenReturn(Optional.of(moi));
        when(candidatureRepository.findById(42L)).thenReturn(Optional.of(candidature));
    }

    @Test
    void le_titulaire_du_dossier_connait_le_montant_avant_de_payer() {
        Candidat moi = new Candidat();
        moi.setId(1L);
        candidature.setCandidat(moi);
        var frais = new PaiementService.FraisDossier(new BigDecimal("25000"), "XOF");
        when(paiementService.fraisDossierPour(42L)).thenReturn(frais);

        ResponseEntity<?> reponse = controller.fraisDossier(42L, authentication);

        assertThat(reponse.getStatusCode().value()).isEqualTo(200);
        assertThat(reponse.getBody()).isEqualTo(frais);
    }

    @Test
    void le_dossier_d_un_autre_candidat_est_refuse() {
        ResponseEntity<?> reponse = controller.fraisDossier(42L, authentication);

        assertThat(reponse.getStatusCode().value()).isEqualTo(403);
        verify(paiementService, never()).fraisDossierPour(42L);
    }
}
