package inpg.sn.lms.espace_etudiant.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.espace_etudiant.dto.ForumSujetDto;
import inpg.sn.lms.espace_etudiant.entity.ForumLecture;
import inpg.sn.lms.espace_etudiant.entity.ForumMessage;
import inpg.sn.lms.espace_etudiant.entity.ForumSujet;
import inpg.sn.lms.espace_etudiant.repository.CoursRepository;
import inpg.sn.lms.espace_etudiant.repository.ForumLectureRepository;
import inpg.sn.lms.espace_etudiant.repository.ForumMessageRepository;
import inpg.sn.lms.espace_etudiant.repository.ForumSujetRepository;
import inpg.sn.lms.espace_etudiant.repository.NotificationRepository;
import inpg.sn.lms.espace_etudiant.service.ForumVisibiliteService;
import inpg.sn.lms.espace_etudiant.service.HabilitationCoursService;
import inpg.sn.lms.espace_etudiant.service.MentionForumService;
import inpg.sn.lms.espace_etudiant.service.PerimetreFormateurService;
import inpg.sn.lms.espace_etudiant.service.UtilisateurCourantService;

/**
 * Pastille de messages non lus du forum (façon WhatsApp) : un message compte comme
 * non lu s'il vient d'un autre et date d'après la dernière ouverture du fil.
 */
class ForumControllerNonLusTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 10, 8, 9, 0);

    private ForumSujetRepository sujetRepository;
    private ForumMessageRepository messageRepository;
    private ForumLectureRepository lectureRepository;
    private UtilisateurCourantService utilisateurCourant;
    private ForumVisibiliteService visibilite;
    private ForumController controller;
    private Authentication authentication;
    private User moi;
    private User autre;

    @BeforeEach
    void setUp() {
        sujetRepository = mock(ForumSujetRepository.class);
        messageRepository = mock(ForumMessageRepository.class);
        lectureRepository = mock(ForumLectureRepository.class);
        utilisateurCourant = mock(UtilisateurCourantService.class);
        visibilite = mock(ForumVisibiliteService.class);
        controller = new ForumController(sujetRepository, messageRepository, lectureRepository,
                mock(CoursRepository.class), utilisateurCourant, mock(HabilitationCoursService.class),
                mock(MentionForumService.class), visibilite, mock(NotificationRepository.class),
                mock(PerimetreFormateurService.class), "uploads");
        authentication = mock(Authentication.class);
        moi = utilisateur(1L);
        autre = utilisateur(2L);
        when(utilisateurCourant.utilisateur(authentication)).thenReturn(moi);
    }

    private static User utilisateur(long id) {
        User user = new User("u" + id, "hash", "u" + id + "@inpg.sn");
        user.setId(id);
        return user;
    }

    private static ForumSujet sujet(long id, User auteur) {
        ForumSujet sujet = new ForumSujet("Sujet " + id, (inpg.sn.lms.espace_etudiant.entity.Cours) null, auteur);
        sujet.setId(id);
        sujet.setDateCreation(T0);
        return sujet;
    }

    private ForumMessage message(ForumSujet sujet, User auteur, int minutes) {
        ForumMessage message = new ForumMessage(sujet, auteur, "m" + minutes);
        message.setDateCreation(T0.plusMinutes(minutes));
        return message;
    }

    private ForumLecture lecture(long sujetId, int minutes) {
        ForumLecture lecture = new ForumLecture();
        lecture.setUserId(1L);
        lecture.setSujetId(sujetId);
        lecture.setDerniereLecture(T0.plusMinutes(minutes));
        return lecture;
    }

    @Test
    @DisplayName("Liste : ne compte que les messages d'un autre arrivés après la dernière ouverture")
    @SuppressWarnings("unchecked")
    void listeCompteLesMessagesNonLus() {
        ForumSujet ouvert = sujet(10L, autre);
        ForumSujet jamaisOuvert = sujet(11L, autre);
        ForumSujet seulementLesMiens = sujet(12L, moi);
        when(visibilite.sujetsVisibles(authentication)).thenReturn(List.of(ouvert, jamaisOuvert, seulementLesMiens));
        // Ouvert à T0+2 : m1 (T0+1) est lu, m2 (T0+3) ne l'est pas, m3 est le mien.
        when(messageRepository.findBySujetIdOrderByDateCreationAsc(10L)).thenReturn(List.of(
                message(ouvert, autre, 1), message(ouvert, autre, 3), message(ouvert, moi, 4)));
        when(messageRepository.findBySujetIdOrderByDateCreationAsc(11L)).thenReturn(List.of(
                message(jamaisOuvert, autre, 1), message(jamaisOuvert, autre, 2)));
        when(messageRepository.findBySujetIdOrderByDateCreationAsc(12L)).thenReturn(List.of(
                message(seulementLesMiens, moi, 1), message(seulementLesMiens, moi, 2)));
        when(lectureRepository.findByUserId(1L)).thenReturn(List.of(lecture(10L, 2)));

        ResponseEntity<?> reponse = controller.sujets(authentication);

        List<ForumSujetDto> dtos = (List<ForumSujetDto>) reponse.getBody();
        assertThat(dtos).extracting(ForumSujetDto::id, ForumSujetDto::nbNonLus)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(10L, 1L),
                        org.assertj.core.groups.Tuple.tuple(11L, 2L),
                        org.assertj.core.groups.Tuple.tuple(12L, 0L));
    }

    @Test
    @DisplayName("Ouvrir un fil le marque comme lu, et la réponse montre déjà 0 non lu")
    @SuppressWarnings("unchecked")
    void ouvrirUnFilLeMarqueCommeLu() {
        ForumSujet fil = sujet(10L, autre);
        when(sujetRepository.findById(10L)).thenReturn(Optional.of(fil));
        when(visibilite.estVisible(fil, authentication)).thenReturn(true);
        when(messageRepository.findBySujetIdOrderByDateCreationAsc(10L))
                .thenReturn(List.of(message(fil, autre, 1), message(fil, autre, 2)));

        ResponseEntity<?> reponse = controller.fil(10L, authentication);

        verify(lectureRepository).marquerLu(eq(1L), eq(10L), any(LocalDateTime.class));
        ForumSujetDto dto = (ForumSujetDto) ((Map<String, Object>) reponse.getBody()).get("sujet");
        assertThat(dto.nbNonLus()).isZero();
    }

    @Test
    @DisplayName("Un fil interdit n'est pas marqué lu : la lecture ne contourne pas la visibilité")
    void filInterditNEstPasMarqueLu() {
        ForumSujet fil = sujet(10L, autre);
        when(sujetRepository.findById(10L)).thenReturn(Optional.of(fil));
        when(visibilite.estVisible(fil, authentication)).thenReturn(false);

        ResponseEntity<?> reponse = controller.fil(10L, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(lectureRepository, never()).marquerLu(any(), any(), any());
    }
}
