package inpg.sn.lms.recrutement.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import inpg.sn.lms.recrutement.entity.SessionCandidature;
import inpg.sn.lms.recrutement.repository.ProgramRepository;
import inpg.sn.lms.recrutement.repository.SessionCandidatureRepository;

/**
 * Avant cette story, {@code GET}/{@code PUT /api/settings/rules} renvoyaient
 * une constante (60/70) jamais persistée nulle part (repéré lors d'un audit
 * complet des endpoints). Le seuil réel existe déjà sur la session de
 * candidature ({@link SessionCandidature#getSeuilReussiteTest()}) : ces
 * endpoints le lisent et l'écrivent désormais réellement.
 */
@ExtendWith(MockitoExtension.class)
class SettingsControllerTest {

    @Mock
    private ProgramRepository programRepo;
    @Mock
    private SessionCandidatureRepository sessionRepo;

    private SettingsController controller;

    @BeforeEach
    void setUp() {
        controller = new SettingsController(programRepo, sessionRepo);
    }

    @Test
    @DisplayName("Sans session ouverte : seuil absent, pas d'erreur")
    void getRules_sans_session_ouverte_renvoie_nul() {
        when(sessionRepo.findTopByEstOuverteTrueOrderByIdDesc()).thenReturn(null);

        ResponseEntity<Map<String, Object>> reponse = controller.getRules();

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reponse.getBody()).containsEntry("seuilTests", null);
    }

    @Test
    @DisplayName("Avec une session ouverte : renvoie son seuil réel")
    void getRules_avec_session_ouverte_renvoie_le_seuil_reel() {
        SessionCandidature session = new SessionCandidature();
        session.setId(3L);
        session.setSeuilReussiteTest(12f);
        when(sessionRepo.findTopByEstOuverteTrueOrderByIdDesc()).thenReturn(session);

        ResponseEntity<Map<String, Object>> reponse = controller.getRules();

        assertThat(reponse.getBody()).containsEntry("seuilTests", 12f).containsEntry("sessionId", 3L);
    }

    @Test
    @DisplayName("Mise à jour sans session ouverte : 409, rien enregistré")
    void updateRules_sans_session_ouverte_est_refusee() {
        when(sessionRepo.findTopByEstOuverteTrueOrderByIdDesc()).thenReturn(null);

        ResponseEntity<?> reponse = controller.updateRules(Map.of("seuilTests", 11));

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(sessionRepo, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Mise à jour valide : le seuil de la session ouverte est réellement persisté")
    void updateRules_persiste_reellement_le_seuil() {
        SessionCandidature session = new SessionCandidature();
        session.setId(3L);
        session.setSeuilReussiteTest(10f);
        when(sessionRepo.findTopByEstOuverteTrueOrderByIdDesc()).thenReturn(session);

        controller.updateRules(Map.of("seuilTests", 13));

        assertThat(session.getSeuilReussiteTest()).isEqualTo(13f);
        verify(sessionRepo).save(session);
    }
}
