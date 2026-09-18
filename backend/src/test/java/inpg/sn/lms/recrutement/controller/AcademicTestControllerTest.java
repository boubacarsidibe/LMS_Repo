package inpg.sn.lms.recrutement.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import inpg.sn.lms.auth.entity.Candidat;
import inpg.sn.lms.recrutement.entity.Candidature;
import inpg.sn.lms.recrutement.entity.TestAcademique;
import inpg.sn.lms.recrutement.repository.CandidatureRepository;
import inpg.sn.lms.recrutement.repository.TestAcademiqueRepository;

/**
 * Avant cette story, {@code createExam} lisait {@code SessionCandidature.getCandidatures()},
 * une collection {@code @OneToMany} paresseuse, en dehors de toute transaction
 * (open-in-view=false) : {@code LazyInitializationException} en production dès qu'une
 * session avait au moins une candidature (repéré lors d'un audit externe, même bug que
 * l'ancien {@code SessionController.java:57}, supprimé car mort). Le correctif interroge
 * directement {@link CandidatureRepository#findBySessionCandidatureId} — ces tests
 * vérifient que la session n'est plus touchée du tout, lazy ou non.
 */
@ExtendWith(MockitoExtension.class)
class AcademicTestControllerTest {

    @Mock
    private TestAcademiqueRepository testRepo;
    @Mock
    private CandidatureRepository candidatureRepo;

    private AcademicTestController controller;

    @BeforeEach
    void setUp() {
        controller = new AcademicTestController(testRepo, candidatureRepo);
    }

    @Test
    @DisplayName("Sans sessionId : aucune candidature interrogée, aucun test créé")
    void createExam_sansSessionId_neCreeAucunTest() {
        ResponseEntity<Map<String, Object>> reponse = controller.createExam(Map.of("nomEpreuve", "Culture générale"));

        assertThat(reponse.getBody()).containsEntry("nomEpreuve", "Culture générale");
        verify(candidatureRepo, never()).findBySessionCandidatureId(any());
        verify(testRepo, never()).save(any());
    }

    @Test
    @DisplayName("Avec sessionId : une épreuve créée par candidature de la session, via une requête directe")
    void createExam_avecSessionId_creeUneEpreuveParCandidature() {
        Candidat candidat1 = new Candidat();
        candidat1.setId(101L);
        Candidat candidat2 = new Candidat();
        candidat2.setId(102L);
        Candidature candidature1 = new Candidature();
        candidature1.setCandidat(candidat1);
        Candidature candidature2 = new Candidature();
        candidature2.setCandidat(candidat2);
        when(candidatureRepo.findBySessionCandidatureId(5L)).thenReturn(List.of(candidature1, candidature2));

        ResponseEntity<Map<String, Object>> reponse = controller.createExam(
                Map.of("nomEpreuve", "Culture scientifique", "sessionId", 5, "coefficient", 2));

        assertThat(reponse.getBody()).containsEntry("nomEpreuve", "Culture scientifique");
        ArgumentCaptor<TestAcademique> captor = ArgumentCaptor.forClass(TestAcademique.class);
        verify(testRepo, times(2)).save(captor.capture());
        List<TestAcademique> crees = captor.getAllValues();
        assertThat(crees).extracting(TestAcademique::getCandidat).containsExactlyInAnyOrder(candidat1, candidat2);
        assertThat(crees).allSatisfy(t -> {
            assertThat(t.getNomEpreuve()).isEqualTo("Culture scientifique");
            assertThat(t.getCoefficient()).isEqualTo(2f);
        });
        // La session elle-même n'est jamais chargée : seule la requête ciblée sur les
        // candidatures est utilisée, donc aucune collection paresseuse n'est en jeu.
        verify(candidatureRepo).findBySessionCandidatureId(5L);
    }

    @Test
    @DisplayName("Session sans aucune candidature : réponse OK, aucun test créé")
    void createExam_sessionSansCandidature_neCreeAucunTest() {
        when(candidatureRepo.findBySessionCandidatureId(9L)).thenReturn(List.of());

        ResponseEntity<Map<String, Object>> reponse = controller.createExam(
                Map.of("nomEpreuve", "Mathématiques appliquées", "sessionId", 9));

        assertThat(reponse.getBody()).containsEntry("message", "Épreuve créée avec succès");
        verify(testRepo, never()).save(any());
    }
}
