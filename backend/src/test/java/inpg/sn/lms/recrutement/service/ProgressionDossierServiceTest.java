package inpg.sn.lms.recrutement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import inpg.sn.lms.recrutement.entity.Candidature;
import inpg.sn.lms.recrutement.entity.Entretien;
import inpg.sn.lms.recrutement.entity.SessionCandidature;
import inpg.sn.lms.recrutement.entity.TestAcademique;
import inpg.sn.lms.recrutement.enums.EnumEtapeQms;
import inpg.sn.lms.recrutement.enums.EnumStatutCandidature;
import inpg.sn.lms.recrutement.repository.CandidatureRepository;
import inpg.sn.lms.recrutement.repository.EntretienRepository;
import inpg.sn.lms.recrutement.repository.TestAcademiqueRepository;

/**
 * Le pipeline d'admission s'arrêtait à l'étape du test : depuis la migration vers vingt statuts, plus rien
 * n'attribuait TEST_REUSSI, TEST_ECHOUE ni ENTRETIEN_EFFECTUE. Un dossier « admis au test » (CONVOQUE_TEST) ne pouvait
 * donc ni être convoqué à l'entretien ni être admis — « Transition de statut non autorisée : CONVOQUE_TEST →
 * CONVOQUE_ENTRETIEN » — et saisir une note n'avait aucun effet sur le dossier. Ces tests figent la règle qui
 * comble ce vide : le résultat se déduit des notes, au seuil de la session.
 */
@ExtendWith(MockitoExtension.class)
class ProgressionDossierServiceTest {

    private static final long CANDIDAT = 7L;

    @Mock private CandidatureRepository candidatureRepository;
    @Mock private TestAcademiqueRepository testAcademiqueRepository;
    @Mock private EntretienRepository entretienRepository;
    @Mock private JournalEtapeService journalEtapeService;

    @InjectMocks private ProgressionDossierService service;

    @BeforeEach
    void setUp() {
        lenient().when(candidatureRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private Candidature dossier(EnumStatutCandidature statut, float seuil) {
        SessionCandidature session = new SessionCandidature();
        session.setSeuilReussiteTest(seuil);
        Candidature c = new Candidature();
        c.setStatut(statut);
        c.setSessionCandidature(session);
        when(candidatureRepository.findByCandidatId(CANDIDAT)).thenReturn(List.of(c));
        return c;
    }

    private static TestAcademique epreuve(Float note, Float coefficient) {
        TestAcademique t = new TestAcademique();
        t.setNoteObtenue(note);
        t.setCoefficient(coefficient);
        return t;
    }

    private static Entretien entretien(Float note) {
        Entretien e = new Entretien();
        e.setNoteEntretien(note);
        return e;
    }

    // ---------------------------------------------------------------- épreuves écrites

    @Test
    @DisplayName("Toutes les épreuves notées, moyenne au-dessus du seuil : le dossier passe en TEST_REUSSI")
    void testReussi() {
        Candidature c = dossier(EnumStatutCandidature.CONVOQUE_TEST, 10f);
        when(testAcademiqueRepository.findByCandidatId(CANDIDAT)).thenReturn(List.of(epreuve(14f, 1f), epreuve(12f, 1f)));

        service.apresTests(CANDIDAT);

        assertEquals(EnumStatutCandidature.TEST_REUSSI, c.getStatut());
        assertEquals(EnumStatutCandidature.CONVOQUE_TEST, c.getStatutPrecedent());
        verify(journalEtapeService).enregistrer(eq(c), eq(EnumEtapeQms.EVALUATION),
                eq(EnumStatutCandidature.TEST_REUSSI), eq(null), any());
    }

    @Test
    @DisplayName("Moyenne sous le seuil de la session : TEST_ECHOUE")
    void testEchoue() {
        Candidature c = dossier(EnumStatutCandidature.CONVOQUE_TEST, 10f);
        when(testAcademiqueRepository.findByCandidatId(CANDIDAT)).thenReturn(List.of(epreuve(6f, 1f), epreuve(7.5f, 2f)));

        service.apresTests(CANDIDAT);

        assertEquals(EnumStatutCandidature.TEST_ECHOUE, c.getStatut());
    }

    @Test
    @DisplayName("Le seuil est celui de la SESSION, pas une constante : 12 de moyenne échoue à un seuil de 14")
    void seuilDeLaSession() {
        Candidature c = dossier(EnumStatutCandidature.CONVOQUE_TEST, 14f);
        when(testAcademiqueRepository.findByCandidatId(CANDIDAT)).thenReturn(List.of(epreuve(12f, 1f)));

        service.apresTests(CANDIDAT);

        assertEquals(EnumStatutCandidature.TEST_ECHOUE, c.getStatut());
    }

    @Test
    @DisplayName("La moyenne est pondérée par les coefficients : 18 (coef 1) et 6 (coef 2) donnent 10, pas 12")
    void moyennePonderee() {
        assertEquals(10.0, ProgressionDossierService.moyennePonderee(List.of(epreuve(18f, 1f), epreuve(6f, 2f))), 1e-9);
    }

    @Test
    @DisplayName("Tant qu'une épreuve n'a pas de note, le dossier reste en attente — aucun résultat n'est décidé")
    void notesIncompletes() {
        Candidature c = dossier(EnumStatutCandidature.CONVOQUE_TEST, 10f);
        when(testAcademiqueRepository.findByCandidatId(CANDIDAT)).thenReturn(List.of(epreuve(15f, 1f), epreuve(null, 1f)));

        service.apresTests(CANDIDAT);

        assertEquals(EnumStatutCandidature.CONVOQUE_TEST, c.getStatut());
        verify(candidatureRepository, never()).save(any());
        assertNull(ProgressionDossierService.moyennePonderee(List.of(epreuve(15f, 1f), epreuve(null, 1f))));
    }

    @Test
    @DisplayName("Corriger une note erronée peut renverser le résultat : TEST_ECHOUE redevient TEST_REUSSI")
    void correctionRenverseLeResultat() {
        Candidature c = dossier(EnumStatutCandidature.TEST_ECHOUE, 10f);
        when(testAcademiqueRepository.findByCandidatId(CANDIDAT)).thenReturn(List.of(epreuve(13f, 1f)));

        service.apresTests(CANDIDAT);

        assertEquals(EnumStatutCandidature.TEST_REUSSI, c.getStatut());
    }

    @Test
    @DisplayName("Effacer une note d'un dossier TEST_REUSSI le remet en attente (CONVOQUE_TEST)")
    void noteEffacee() {
        Candidature c = dossier(EnumStatutCandidature.TEST_REUSSI, 10f);
        when(testAcademiqueRepository.findByCandidatId(CANDIDAT)).thenReturn(List.of(epreuve(null, 1f)));

        service.apresTests(CANDIDAT);

        assertEquals(EnumStatutCandidature.CONVOQUE_TEST, c.getStatut());
    }

    @Test
    @DisplayName("Un dossier déjà plus avancé n'est jamais ramené en arrière par une note de test modifiée")
    void dossierAvanceIntouche() {
        Candidature c = dossier(EnumStatutCandidature.CONVOQUE_ENTRETIEN, 10f);

        service.apresTests(CANDIDAT);

        assertEquals(EnumStatutCandidature.CONVOQUE_ENTRETIEN, c.getStatut());
        verify(candidatureRepository, never()).save(any());
        verify(testAcademiqueRepository, never()).findByCandidatId(any());
    }

    @Test
    @DisplayName("Un candidat sans dossier : rien à faire, et surtout pas d'erreur")
    void sansDossier() {
        when(candidatureRepository.findByCandidatId(CANDIDAT)).thenReturn(List.of());

        service.apresTests(CANDIDAT);
        service.apresEntretien(CANDIDAT);

        verify(candidatureRepository, never()).save(any());
    }

    // ---------------------------------------------------------------- entretien

    @Test
    @DisplayName("La note d'entretien saisie fait passer le dossier en ENTRETIEN_EFFECTUE — prêt pour la délibération")
    void entretienEffectue() {
        Candidature c = dossier(EnumStatutCandidature.CONVOQUE_ENTRETIEN, 10f);
        when(entretienRepository.findByCandidatId(CANDIDAT)).thenReturn(List.of(entretien(15f)));

        service.apresEntretien(CANDIDAT);

        assertEquals(EnumStatutCandidature.ENTRETIEN_EFFECTUE, c.getStatut());
    }

    @Test
    @DisplayName("Un entretien planifié mais non noté laisse le dossier en CONVOQUE_ENTRETIEN")
    void entretienNonNote() {
        Candidature c = dossier(EnumStatutCandidature.CONVOQUE_ENTRETIEN, 10f);
        when(entretienRepository.findByCandidatId(CANDIDAT)).thenReturn(List.of(entretien(null)));

        service.apresEntretien(CANDIDAT);

        assertEquals(EnumStatutCandidature.CONVOQUE_ENTRETIEN, c.getStatut());
        verify(candidatureRepository, never()).save(any());
    }

    @Test
    @DisplayName("Effacer la note d'entretien remet le dossier en CONVOQUE_ENTRETIEN")
    void noteEntretienEffacee() {
        Candidature c = dossier(EnumStatutCandidature.ENTRETIEN_EFFECTUE, 10f);
        when(entretienRepository.findByCandidatId(CANDIDAT)).thenReturn(List.of(entretien(null)));

        service.apresEntretien(CANDIDAT);

        assertEquals(EnumStatutCandidature.CONVOQUE_ENTRETIEN, c.getStatut());
    }

    @Test
    @DisplayName("Un candidat déjà admis n'est pas touché par une note d'entretien corrigée")
    void admisIntouche() {
        Candidature c = dossier(EnumStatutCandidature.ADMIS, 10f);

        service.apresEntretien(CANDIDAT);

        assertEquals(EnumStatutCandidature.ADMIS, c.getStatut());
        verify(candidatureRepository, never()).save(any());
    }
}
