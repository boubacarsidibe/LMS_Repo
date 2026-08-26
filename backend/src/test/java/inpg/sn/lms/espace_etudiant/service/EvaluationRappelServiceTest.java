package inpg.sn.lms.espace_etudiant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.EtudiantRepository;
import inpg.sn.lms.espace_etudiant.entity.Cours;
import inpg.sn.lms.espace_etudiant.entity.Notification;
import inpg.sn.lms.espace_etudiant.entity.RetourEvaluation;
import inpg.sn.lms.espace_etudiant.repository.NotificationRepository;
import inpg.sn.lms.espace_etudiant.repository.RetourEvaluationRepository;

/**
 * Rappel automatique d'évaluation (TDR §G, besoin B4). Le job est déclenché par
 * {@code @Scheduled} en production ; ici, {@code rappelerEvaluationsEnAttente()}
 * est appelée directement, ce qui suffit à couvrir sa logique (le déclenchement
 * planifié lui-même relève de la configuration Spring, pas du comportement).
 */
@ExtendWith(MockitoExtension.class)
class EvaluationRappelServiceTest {

    @Mock
    private EtudiantRepository etudiantRepository;
    @Mock
    private RetourEvaluationRepository retourRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private HabilitationCoursService habilitation;

    private EvaluationRappelService service;

    @BeforeEach
    void setUp() {
        service = new EvaluationRappelService(etudiantRepository, retourRepository, notificationRepository,
                habilitation);
    }

    private Etudiant etudiantAvecCompte(long id) {
        User user = new User("etu" + id, "hash", "etu" + id + "@inpg.sn");
        user.setId(id);
        Etudiant etudiant = new Etudiant(user, "Nom", "Prenom", "77" + id);
        etudiant.setId(id);
        return etudiant;
    }

    private Cours cours(long id) {
        Cours cours = mock(Cours.class);
        lenient().when(cours.getId()).thenReturn(id);
        return cours;
    }

    @Test
    @DisplayName("Étudiant sans compte utilisateur associé : jamais notifié")
    void etudiantSansCompteNestJamaisNotifie() {
        Etudiant etudiant = new Etudiant(null, "Nom", "Prenom", "770000000");
        when(etudiantRepository.findAll()).thenReturn(List.of(etudiant));

        service.rappelerEvaluationsEnAttente();

        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Aucun cours autorisé : jamais notifié")
    void etudiantSansCoursAutoriseNestJamaisNotifie() {
        Etudiant etudiant = etudiantAvecCompte(1L);
        when(etudiantRepository.findAll()).thenReturn(List.of(etudiant));
        when(habilitation.coursAutorises(etudiant)).thenReturn(List.of());

        service.rappelerEvaluationsEnAttente();

        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Tous les cours autorisés sont déjà évalués : jamais notifié")
    void etudiantAyantToutEvalueNestPasRelance() {
        Etudiant etudiant = etudiantAvecCompte(1L);
        Cours coursUnique = cours(100L);
        when(etudiantRepository.findAll()).thenReturn(List.of(etudiant));
        when(habilitation.coursAutorises(etudiant)).thenReturn(List.of(coursUnique));

        RetourEvaluation retour = new RetourEvaluation();
        retour.setCours(coursUnique);
        when(retourRepository.findByEtudiantIdOrderByDateCreationDesc(1L)).thenReturn(List.of(retour));

        service.rappelerEvaluationsEnAttente();

        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cours en attente d'évaluation, hors période de repos : une notification agrégée est envoyée")
    void etudiantAvecCoursEnAttenteEstRelance() {
        Etudiant etudiant = etudiantAvecCompte(1L);
        Cours coursEvalue = cours(100L);
        Cours coursEnAttente1 = cours(101L);
        Cours coursEnAttente2 = cours(102L);
        when(etudiantRepository.findAll()).thenReturn(List.of(etudiant));
        when(habilitation.coursAutorises(etudiant)).thenReturn(List.of(coursEvalue, coursEnAttente1, coursEnAttente2));

        RetourEvaluation retour = new RetourEvaluation();
        retour.setCours(coursEvalue);
        when(retourRepository.findByEtudiantIdOrderByDateCreationDesc(1L)).thenReturn(List.of(retour));
        when(notificationRepository.existsByDestinataire_IdAndTypeAndDateCreationAfter(
                eq(1L), eq(Notification.EVALUATION_RAPPEL), any(LocalDateTime.class))).thenReturn(false);

        service.rappelerEvaluationsEnAttente();

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getMessage()).isEqualTo("2 cours attendent votre évaluation.");
        assertThat(captor.getValue().getType()).isEqualTo(Notification.EVALUATION_RAPPEL);
    }

    @Test
    @DisplayName("Déjà relancé dans la fenêtre de repos : pas de seconde notification")
    void etudiantDejaRelanceRecemmentNestPasRenotifie() {
        Etudiant etudiant = etudiantAvecCompte(1L);
        Cours coursEnAttente = cours(101L);
        when(etudiantRepository.findAll()).thenReturn(List.of(etudiant));
        when(habilitation.coursAutorises(etudiant)).thenReturn(List.of(coursEnAttente));
        when(retourRepository.findByEtudiantIdOrderByDateCreationDesc(1L)).thenReturn(List.of());
        when(notificationRepository.existsByDestinataire_IdAndTypeAndDateCreationAfter(
                eq(1L), eq(Notification.EVALUATION_RAPPEL), any(LocalDateTime.class))).thenReturn(true);

        service.rappelerEvaluationsEnAttente();

        verify(notificationRepository, never()).save(any());
    }
}
