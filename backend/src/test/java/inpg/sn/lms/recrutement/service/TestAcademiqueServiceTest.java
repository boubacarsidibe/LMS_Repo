package inpg.sn.lms.recrutement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import inpg.sn.lms.auth.entity.Candidat;
import inpg.sn.lms.auth.repository.CandidatRepository;
import inpg.sn.lms.recrutement.dto.TestAcademiqueRequest;
import inpg.sn.lms.recrutement.dto.TestAcademiqueResponse;
import inpg.sn.lms.recrutement.entity.Candidature;
import inpg.sn.lms.recrutement.entity.SessionCandidature;
import inpg.sn.lms.recrutement.entity.TestAcademique;
import inpg.sn.lms.recrutement.repository.CandidatureRepository;
import inpg.sn.lms.recrutement.repository.TestAcademiqueRepository;

/**
 * Le TDR exige que lieu, date, heure et session soient clairement précisés à la
 * convocation. La session n'était jamais retournée par l'API (déduite en silence
 * côté front d'une liste de sessions chargée à part) ; le lieu n'existait pas du
 * tout. Ces deux points sont figés ici.
 */
@ExtendWith(MockitoExtension.class)
class TestAcademiqueServiceTest {

    private static final long ID_CANDIDAT = 1L;

    @Mock private TestAcademiqueRepository testAcademiqueRepository;
    @Mock private CandidatRepository candidatRepository;
    @Mock private CandidatureRepository candidatureRepository;
    @Mock private NotificationService notificationService;
    @Mock private NotificationCandidatService notificationCandidatService;

    @InjectMocks private TestAcademiqueService service;

    private Candidat candidat;

    @BeforeEach
    void setUp() {
        candidat = new Candidat();
        candidat.setId(ID_CANDIDAT);
        lenient().when(candidatRepository.findById(ID_CANDIDAT)).thenReturn(Optional.of(candidat));
        lenient().when(testAcademiqueRepository.save(any())).thenAnswer(inv -> {
            TestAcademique t = inv.getArgument(0);
            t.setId(99L);
            return t;
        });
    }

    @Test
    void le_lieu_est_conserve_a_la_creation() {
        TestAcademiqueRequest request = new TestAcademiqueRequest();
        request.setCandidatId(ID_CANDIDAT);
        request.setNomEpreuve("Test de maths");
        request.setCoefficient(1f);
        request.setLieuOuLien("Salle B12");

        TestAcademiqueResponse reponse = service.creerTest(request);

        assertEquals("Salle B12", reponse.getLieuOuLien());
    }

    @Test
    void la_session_du_candidat_est_exposee_dans_la_reponse() {
        SessionCandidature session = new SessionCandidature();
        session.setId(5L);
        session.setLibelle("Master Data Science 2026");
        Candidature candidature = new Candidature();
        candidature.setSessionCandidature(session);
        when(candidatureRepository.findByCandidatId(ID_CANDIDAT)).thenReturn(List.of(candidature));

        TestAcademiqueRequest request = new TestAcademiqueRequest();
        request.setCandidatId(ID_CANDIDAT);
        request.setNomEpreuve("Test de maths");
        request.setCoefficient(1f);

        TestAcademiqueResponse reponse = service.creerTest(request);

        assertEquals(5L, reponse.getSessionCandidatureId());
        assertEquals("Master Data Science 2026", reponse.getSessionCandidatureLibelle());
    }

    @Test
    void aucune_candidature_ne_laisse_la_session_absente_plutot_que_de_planter() {
        when(candidatureRepository.findByCandidatId(ID_CANDIDAT)).thenReturn(List.of());

        TestAcademiqueRequest request = new TestAcademiqueRequest();
        request.setCandidatId(ID_CANDIDAT);
        request.setNomEpreuve("Test de maths");
        request.setCoefficient(1f);

        TestAcademiqueResponse reponse = service.creerTest(request);

        assertNull(reponse.getSessionCandidatureId());
        assertNull(reponse.getSessionCandidatureLibelle());
    }
}
