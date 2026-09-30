package inpg.sn.lms.espace_etudiant.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.espace_etudiant.entity.Cours;
import inpg.sn.lms.espace_etudiant.entity.ForumMessage;
import inpg.sn.lms.espace_etudiant.entity.ForumSujet;
import inpg.sn.lms.espace_etudiant.repository.CoursRepository;
import inpg.sn.lms.espace_etudiant.repository.ForumMessageRepository;
import inpg.sn.lms.espace_etudiant.repository.ForumSujetRepository;
import inpg.sn.lms.espace_etudiant.repository.NotificationRepository;
import inpg.sn.lms.espace_etudiant.service.ForumVisibiliteService;
import inpg.sn.lms.espace_etudiant.service.HabilitationCoursService;
import inpg.sn.lms.espace_etudiant.service.MentionForumService;
import inpg.sn.lms.espace_etudiant.service.UtilisateurCourantService;

/**
 * {@code ForumController} (forum côté étudiant, {@code /api/espace-etudiant/forum})
 * n'avait jusqu'ici aucun test dédié — ni structurel (contrairement à
 * {@code EspaceEtudiantController}, couvert par {@code EspaceEtudiantGardeFousTest})
 * ni comportemental. Ce fichier comble les deux trous à l'occasion de l'ajout des
 * pièces jointes (2026-09-28), sur le même patron que {@code MessagerieControllerTest}.
 */
@ExtendWith(MockitoExtension.class)
class ForumControllerTest {

    private static final List<Class<? extends Annotation>> ANNOTATIONS_DE_ROUTE = List.of(
            GetMapping.class, PostMapping.class, PutMapping.class,
            PatchMapping.class, DeleteMapping.class, RequestMapping.class);

    private static boolean estUneRoute(Method methode) {
        return ANNOTATIONS_DE_ROUTE.stream().anyMatch(methode::isAnnotationPresent);
    }

    @Test
    @DisplayName("Le contrôle de rôle est posé une fois pour toutes au niveau de la classe")
    void le_controle_de_role_est_porte_par_la_classe() {
        assertThat(ForumController.class.isAnnotationPresent(PreAuthorize.class)).isTrue();
    }

    @Test
    @DisplayName("Chaque endpoint ouvre sa transaction")
    void chaque_endpoint_ouvre_sa_transaction() {
        List<String> sansTransaction = Arrays.stream(ForumController.class.getDeclaredMethods())
                .filter(ForumControllerTest::estUneRoute)
                .filter(methode -> !methode.isAnnotationPresent(Transactional.class))
                .map(Method::getName)
                .sorted()
                .toList();

        assertThat(sansTransaction)
                .as("Ces endpoints n'ouvrent aucune transaction : toute lecture d'une association "
                        + "paresseuse y renverra une erreur 500 en production.")
                .isEmpty();
    }

    @Mock
    private ForumSujetRepository sujetRepository;
    @Mock
    private ForumMessageRepository messageRepository;
    @Mock
    private CoursRepository coursRepository;
    @Mock
    private UtilisateurCourantService utilisateurCourant;
    @Mock
    private HabilitationCoursService habilitation;
    @Mock
    private MentionForumService mentions;
    @Mock
    private ForumVisibiliteService visibilite;
    @Mock
    private NotificationRepository notificationRepository;

    private ForumController controller;
    private Authentication authentication;
    private User auteur;
    private ForumSujet sujet;

    @BeforeEach
    void preparer() {
        controller = new ForumController(sujetRepository, messageRepository, coursRepository, utilisateurCourant,
                habilitation, mentions, visibilite, notificationRepository, "uploads");

        auteur = new User();
        auteur.setId(5L);
        auteur.setUsername("awa_diallo");
        authentication = new UsernamePasswordAuthenticationToken("awa_diallo", "n/a", List.of());

        sujet = new ForumSujet("Titre", (Cours) null, auteur);
        sujet.setId(7L);
    }

    /**
     * Utilisée seulement par les tests de {@code repondre} : les tests de
     * téléchargement résolvent le sujet via {@code message.getSujet()}, jamais via
     * {@code sujetRepository} ni {@code utilisateurCourant} — un stub posé ici pour
     * tous les tests resterait inutilisé pour eux et ferait échouer le mode strict
     * de Mockito.
     */
    private void connecte() {
        when(utilisateurCourant.utilisateur(authentication)).thenReturn(auteur);
        when(sujetRepository.findById(7L)).thenReturn(Optional.of(sujet));
    }

    // ===================== Répondre =====================

    @Test
    @DisplayName("Répondre à un sujet non visible : 404, rien enregistré")
    void repondre_a_un_sujet_non_visible_est_refuse() {
        connecte();
        when(visibilite.estVisible(sujet, authentication)).thenReturn(false);

        ResponseEntity<?> reponse = controller.repondre(7L, "Bonjour", null, null, null, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("Répondre à un sujet visible, sans pièce jointe : accepté")
    void repondre_a_un_sujet_visible_est_accepte() {
        connecte();
        when(visibilite.estVisible(sujet, authentication)).thenReturn(true);
        when(messageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<?> reponse = controller.repondre(7L, "Bonjour", null, null, null, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(messageRepository).save(any());
    }

    @Test
    @DisplayName("Répondre avec un message vide : rejetée")
    void repondre_avec_contenu_vide_est_rejetee() {
        connecte();
        when(visibilite.estVisible(sujet, authentication)).thenReturn(true);

        ResponseEntity<?> reponse = controller.repondre(7L, "   ", null, null, null, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("Répondre avec une pièce jointe valide : les trois informations sont enregistrées")
    void repondre_avec_piece_jointe_valide_est_acceptee(@TempDir Path repertoireTemp) {
        connecte();
        controller = new ForumController(sujetRepository, messageRepository, coursRepository, utilisateurCourant,
                habilitation, mentions, visibilite, notificationRepository, repertoireTemp.toString());
        when(visibilite.estVisible(sujet, authentication)).thenReturn(true);
        when(messageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        MockMultipartFile fichier = new MockMultipartFile("fichier", "support.pdf", "application/pdf", "contenu".getBytes());

        ResponseEntity<?> reponse = controller.repondre(7L, "Voici le support", null, null, fichier, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ArgumentCaptor<ForumMessage> capture = ArgumentCaptor.forClass(ForumMessage.class);
        verify(messageRepository).save(capture.capture());
        assertThat(capture.getValue().getPieceJointeNomOriginal()).isEqualTo("support.pdf");
        assertThat(capture.getValue().getPieceJointeNomStocke()).endsWith("-support.pdf");
        assertThat(capture.getValue().getPieceJointeTypeContenu()).isEqualTo("application/pdf");
    }

    @Test
    @DisplayName("Répondre avec une pièce jointe d'un format refusé : rejetée, rien enregistré")
    void repondre_avec_piece_jointe_invalide_est_rejetee(@TempDir Path repertoireTemp) {
        connecte();
        controller = new ForumController(sujetRepository, messageRepository, coursRepository, utilisateurCourant,
                habilitation, mentions, visibilite, notificationRepository, repertoireTemp.toString());
        when(visibilite.estVisible(sujet, authentication)).thenReturn(true);
        MockMultipartFile fichier = new MockMultipartFile("fichier", "page.html", "text/html", "<script>1</script>".getBytes());

        ResponseEntity<?> reponse = controller.repondre(7L, "Voici", null, null, fichier, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(messageRepository, never()).save(any());
    }

    // ===================== Télécharger une pièce jointe =====================

    @Test
    @DisplayName("Télécharger la pièce jointe d'un message d'un sujet non visible : introuvable")
    void telecharger_piece_jointe_d_un_sujet_non_visible_est_refuse() {
        ForumMessage message = new ForumMessage(sujet, auteur, "Bonjour");
        message.setPieceJointeNomOriginal("support.pdf");
        message.setPieceJointeNomStocke("abc-support.pdf");
        when(messageRepository.findById(99L)).thenReturn(Optional.of(message));
        when(visibilite.estVisible(sujet, authentication)).thenReturn(false);

        ResponseEntity<?> reponse = controller.telechargerPieceJointe(99L, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Télécharger la pièce jointe d'un message sans pièce jointe : introuvable")
    void telecharger_piece_jointe_absente_est_introuvable() {
        ForumMessage message = new ForumMessage(sujet, auteur, "Sans pièce jointe");
        when(messageRepository.findById(99L)).thenReturn(Optional.of(message));

        ResponseEntity<?> reponse = controller.telechargerPieceJointe(99L, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Télécharger la pièce jointe d'un message d'un sujet visible : servie")
    void telecharger_piece_jointe_d_un_sujet_visible_est_servie(@TempDir Path repertoireTemp) throws Exception {
        controller = new ForumController(sujetRepository, messageRepository, coursRepository, utilisateurCourant,
                habilitation, mentions, visibilite, notificationRepository, repertoireTemp.toString());
        Path dossierMessages = repertoireTemp.resolve("messages");
        Files.createDirectories(dossierMessages);
        Files.writeString(dossierMessages.resolve("abc-support.pdf"), "contenu du support");

        ForumMessage message = new ForumMessage(sujet, auteur, "Bonjour");
        message.setPieceJointeNomOriginal("support.pdf");
        message.setPieceJointeNomStocke("abc-support.pdf");
        message.setPieceJointeTypeContenu("application/pdf");
        when(messageRepository.findById(99L)).thenReturn(Optional.of(message));
        when(visibilite.estVisible(sujet, authentication)).thenReturn(true);

        ResponseEntity<?> reponse = controller.telechargerPieceJointe(99L, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
