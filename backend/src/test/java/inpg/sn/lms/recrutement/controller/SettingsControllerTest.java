package inpg.sn.lms.recrutement.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.ObjectMapper;

import inpg.sn.lms.exception.ResourceNotFoundException;
import inpg.sn.lms.recrutement.dto.ProgramRequest;
import inpg.sn.lms.recrutement.entity.Program;
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
    @DisplayName("Mise à jour avec une valeur non numérique : 400, rien enregistré")
    void updateRules_avec_valeur_non_numerique_est_refusee() {
        SessionCandidature session = new SessionCandidature();
        session.setId(3L);
        when(sessionRepo.findTopByEstOuverteTrueOrderByIdDesc()).thenReturn(session);

        ResponseEntity<?> reponse = controller.updateRules(Map.of("seuilTests", "douze"));

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
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

    // ===================== PROGRAMS =====================

    /**
     * Reproduit la panne réelle de {@code GET /api/settings/programs} : hors
     * transaction, {@code Program.sessions} est une collection paresseuse
     * détachée qui lève {@link LazyInitializationException} dès qu'on la
     * parcourt. Tant que le contrôleur renvoyait l'entité brute, l'échec ne
     * survenait pas dans la méthode mais pendant l'écriture de la réponse —
     * d'où un 500 « Failed to write request » qui ne passait même pas par
     * {@code GlobalExceptionHandler}, et qu'aucun test à mocks ne pouvait
     * voir puisqu'aucun ne sérialisait la réponse.
     */
    private static List<SessionCandidature> collectionParesseuseDetachee() {
        return new AbstractList<>() {
            @Override
            public SessionCandidature get(int index) {
                throw lazy();
            }

            @Override
            public int size() {
                throw lazy();
            }

            @Override
            public Iterator<SessionCandidature> iterator() {
                throw lazy();
            }
        };
    }

    private static LazyInitializationException lazy() {
        return new LazyInitializationException(
                "failed to lazily initialize a collection of role: Program.sessions (no session)");
    }

    private Program programmeAvecSessionsParesseuses() {
        Program program = new Program();
        program.setId(7L);
        program.setTitle("Executive MBA Petrole & Gaz");
        program.setLevel("MBA");
        program.setDepartment("Management");
        program.setSessions(collectionParesseuseDetachee());
        return program;
    }

    @Test
    @DisplayName("La liste des programmes est sérialisable même avec des sessions paresseuses détachées")
    void listPrograms_est_serialisable_hors_transaction() {
        when(programRepo.findAll()).thenReturn(List.of(programmeAvecSessionsParesseuses()));

        // ResponseEntity<?> : compile aussi bien contre l'ancienne signature
        // (List<Program>) que la nouvelle (List<ProgramResponse>), pour que ce
        // test constate réellement la régression au lieu de la contourner.
        ResponseEntity<?> reponse = controller.listPrograms();

        ObjectMapper mapper = new ObjectMapper();
        assertThatCode(() -> mapper.writeValueAsString(reponse.getBody())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("La réponse n'expose ni la collection de sessions ni sa référence retour")
    void listPrograms_n_expose_pas_le_graphe_d_entites() throws Exception {
        when(programRepo.findAll()).thenReturn(List.of(programmeAvecSessionsParesseuses()));

        ResponseEntity<?> reponse = controller.listPrograms();
        String json = new ObjectMapper().writeValueAsString(reponse.getBody());

        assertThat(json).contains("Executive MBA Petrole & Gaz", "MBA", "Management");
        assertThat(json).doesNotContain("sessions");
    }

    @Test
    @DisplayName("Création : seuls les champs du programme sont repris, jamais ses sessions")
    void createProgram_ne_touche_pas_aux_sessions() {
        ProgramRequest requete = new ProgramRequest();
        requete.setTitle("  Cycle Ingenieur Procedes  ");
        requete.setLevel("Ingenieur");
        requete.setDepartment("Genie Industriel");
        when(programRepo.save(org.mockito.ArgumentMatchers.any(Program.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        controller.createProgram(requete);

        org.mockito.ArgumentCaptor<Program> capture = org.mockito.ArgumentCaptor.forClass(Program.class);
        verify(programRepo).save(capture.capture());
        // Le libellé est nettoyé, et la collection reste celle de l'entité neuve :
        // aucune charge utile ne peut y injecter de session (cascade MERGE/PERSIST).
        assertThat(capture.getValue().getTitle()).isEqualTo("Cycle Ingenieur Procedes");
        assertThat(capture.getValue().getSessions()).isEmpty();
    }

    @Test
    @DisplayName("Suppression d'un programme rattaché à des sessions : 409, rien supprimé")
    void deleteProgram_refuse_si_des_sessions_y_sont_rattachees() {
        when(programRepo.existsById(7L)).thenReturn(true);
        when(sessionRepo.countByProgram_Id(7L)).thenReturn(3L);

        ResponseEntity<Map<String, String>> reponse = controller.deleteProgram(7L);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(reponse.getBody()).extracting("message").asString().contains("3 session(s)");
        verify(programRepo, never()).deleteById(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Suppression d'un identifiant inconnu : 404 plutôt qu'un faux succès")
    void deleteProgram_sur_identifiant_inconnu_renvoie_404() {
        when(programRepo.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> controller.deleteProgram(99L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(programRepo, never()).deleteById(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Suppression d'un programme libre : effectuée")
    void deleteProgram_sans_session_rattachee_supprime() {
        when(programRepo.existsById(7L)).thenReturn(true);
        when(sessionRepo.countByProgram_Id(7L)).thenReturn(0L);

        ResponseEntity<Map<String, String>> reponse = controller.deleteProgram(7L);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(programRepo).deleteById(7L);
    }
}
