package inpg.sn.lms.recrutement.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import inpg.sn.lms.auth.entity.Candidat;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.CandidatRepository;
import inpg.sn.lms.auth.repository.UserRepository;
import inpg.sn.lms.recrutement.entity.NotificationCandidat;
import inpg.sn.lms.recrutement.service.NotificationCandidatService;

/**
 * Un compte tout juste créé n'a pas de profil candidat (il naît à la première soumission). Le bandeau de navigation
 * interroge pourtant les notifications dès la connexion : la route répondait 404, une erreur dans la console du navigateur
 * pour chaque nouvel inscrit. « Aucune notification » est une réponse, pas une erreur.
 */
class NotificationControllerTest {

    private NotificationCandidatService service;
    private CandidatRepository candidatRepository;
    private UserRepository userRepository;
    private NotificationController controller;
    private Authentication auth;

    @BeforeEach
    void setUp() {
        service = mock(NotificationCandidatService.class);
        candidatRepository = mock(CandidatRepository.class);
        userRepository = mock(UserRepository.class);
        controller = new NotificationController(service, candidatRepository, userRepository);

        auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("fatou_sene");
        User user = new User();
        user.setId(5L);
        when(userRepository.findByUsername("fatou_sene")).thenReturn(user);
    }

    @Test
    @DisplayName("Sans profil candidat, la liste des notifications est vide (200), pas une erreur 404")
    void listeVideSansProfil() {
        when(candidatRepository.findByUserId(5L)).thenReturn(Optional.empty());

        var reponse = controller.getNotifications(auth);

        assertThat(reponse.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(reponse.getBody()).isEmpty();
    }

    @Test
    @DisplayName("Sans profil candidat, le compteur de notifications non lues vaut zéro")
    void compteurNulSansProfil() {
        when(candidatRepository.findByUserId(5L)).thenReturn(Optional.empty());

        var reponse = controller.countNonLues(auth);

        assertThat(reponse.getBody()).containsEntry("count", 0L);
    }

    @Test
    @DisplayName("Avec un profil candidat, ses notifications sont servies comme avant")
    void notificationsDuCandidat() {
        Candidat candidat = new Candidat();
        candidat.setId(9L);
        when(candidatRepository.findByUserId(5L)).thenReturn(Optional.of(candidat));
        NotificationCandidat n = new NotificationCandidat();
        when(service.getNotificationsByCandidatId(9L)).thenReturn(List.of(n));
        when(service.countNonLues(9L)).thenReturn(3L);

        assertThat(controller.getNotifications(auth).getBody()).containsExactly(n);
        assertThat(controller.countNonLues(auth).getBody()).containsEntry("count", 3L);
    }
}
