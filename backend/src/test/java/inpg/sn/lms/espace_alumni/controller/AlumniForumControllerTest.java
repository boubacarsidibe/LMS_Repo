package inpg.sn.lms.espace_alumni.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.espace_etudiant.dto.ForumMessageCreateRequest;
import inpg.sn.lms.espace_etudiant.dto.ForumSujetCreateRequest;
import inpg.sn.lms.espace_etudiant.entity.ForumMessage;
import inpg.sn.lms.espace_etudiant.entity.ForumSujet;
import inpg.sn.lms.espace_etudiant.entity.PorteeForumSujet;
import inpg.sn.lms.espace_etudiant.repository.ForumMessageRepository;
import inpg.sn.lms.espace_etudiant.repository.ForumSujetRepository;
import inpg.sn.lms.espace_etudiant.service.MentionForumService;
import inpg.sn.lms.espace_etudiant.service.UtilisateurCourantService;

/**
 * Forum de l'espace Alumni : un salon plat, jamais un pont vers l'espace
 * étudiant.
 *
 * <p>Le risque principal, propre à ce contrôleur, est d'atteindre un sujet ou un
 * message de l'espace étudiant en devinant simplement son identifiant — les deux
 * espaces partagent les mêmes entités ({@code ForumSujet}/{@code ForumMessage}),
 * distinguées uniquement par {@code portee}. Chaque endpoint qui charge un sujet
 * ou un message par id doit donc vérifier sa portée avant de répondre.
 */
@ExtendWith(MockitoExtension.class)
class AlumniForumControllerTest {

    @Mock
    private ForumSujetRepository sujetRepository;
    @Mock
    private ForumMessageRepository messageRepository;
    @Mock
    private UtilisateurCourantService utilisateurCourant;
    @Mock
    private MentionForumService mentions;
    @Mock
    private Authentication authentication;

    private AlumniForumController controller;
    private User alumni;

    @BeforeEach
    void setUp() {
        controller = new AlumniForumController(sujetRepository, messageRepository, utilisateurCourant, mentions);
        alumni = new User();
        alumni.setId(42L);
    }

    private ForumSujet sujetDe(Long id, PorteeForumSujet portee) {
        ForumSujet sujet = portee == PorteeForumSujet.ALUMNI
                ? new ForumSujet("Titre", alumni, PorteeForumSujet.ALUMNI)
                : new ForumSujet("Titre", null, alumni);
        setId(sujet, id);
        return sujet;
    }

    private void setId(Object entite, Long id) {
        try {
            var champ = entite.getClass().getDeclaredField("id");
            champ.setAccessible(true);
            champ.set(entite, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Impossible de fixer l'id", e);
        }
    }

    @Test
    @DisplayName("Un sujet ouvert par un alumni est toujours général et porté ALUMNI")
    void ouvrirSujet_toujours_general_et_alumni() {
        when(utilisateurCourant.utilisateur(authentication)).thenReturn(alumni);
        when(sujetRepository.save(any(ForumSujet.class))).thenAnswer(a -> a.getArgument(0));
        ForumSujetCreateRequest requete = new ForumSujetCreateRequest("Retrouvailles", 999L, "On se retrouve ?");

        ResponseEntity<?> reponse = controller.ouvrirSujet(requete, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        org.mockito.ArgumentCaptor<ForumSujet> capteur = org.mockito.ArgumentCaptor.forClass(ForumSujet.class);
        verify(sujetRepository).save(capteur.capture());
        assertThat(capteur.getValue().getPortee()).isEqualTo(PorteeForumSujet.ALUMNI);
        // Le coursId envoyé par le client (999L) est ignoré : un alumni n'a pas de cours.
        assertThat(capteur.getValue().getCours()).isNull();
    }

    @Test
    @DisplayName("Un sujet de l'espace étudiant est inatteignable par son identifiant")
    void fil_refuse_un_sujet_etudiant() {
        ForumSujet sujetEtudiant = sujetDe(7L, PorteeForumSujet.ETUDIANT);
        when(sujetRepository.findById(7L)).thenReturn(Optional.of(sujetEtudiant));

        ResponseEntity<?> reponse = controller.fil(7L, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Un sujet alumni existant renvoie son fil normalement")
    void fil_accepte_un_sujet_alumni() {
        ForumSujet sujetAlumni = sujetDe(8L, PorteeForumSujet.ALUMNI);
        when(sujetRepository.findById(8L)).thenReturn(Optional.of(sujetAlumni));
        when(messageRepository.findBySujetIdOrderByDateCreationAsc(8L)).thenReturn(java.util.List.of());
        lenient().when(utilisateurCourant.utilisateur(authentication)).thenReturn(alumni);
        lenient().when(utilisateurCourant.nomAffiche(any())).thenReturn("Awa Ndoye");

        ResponseEntity<?> reponse = controller.fil(8L, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Répondre à un sujet de l'espace étudiant échoue, même avec un contenu valide")
    void repondre_refuse_un_sujet_etudiant() {
        ForumSujet sujetEtudiant = sujetDe(7L, PorteeForumSujet.ETUDIANT);
        when(utilisateurCourant.utilisateur(authentication)).thenReturn(alumni);
        when(sujetRepository.findById(7L)).thenReturn(Optional.of(sujetEtudiant));

        ResponseEntity<?> reponse = controller.repondre(7L, new ForumMessageCreateRequest("Bonjour", java.util.List.of()),
                authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Supprimer un message d'un fil étudiant échoue, même si on en est l'auteur")
    void supprimerMessage_refuse_un_message_etudiant() {
        ForumSujet sujetEtudiant = sujetDe(7L, PorteeForumSujet.ETUDIANT);
        ForumMessage message = new ForumMessage(sujetEtudiant, alumni, "Contenu");
        setId(message, 55L);
        when(messageRepository.findById(55L)).thenReturn(Optional.of(message));

        ResponseEntity<?> reponse = controller.supprimerMessage(55L, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Supprimer le message d'autrui échoue, même dans un fil alumni")
    void supprimerMessage_refuse_le_message_d_autrui() {
        ForumSujet sujetAlumni = sujetDe(8L, PorteeForumSujet.ALUMNI);
        User autreAlumni = new User();
        autreAlumni.setId(999L);
        ForumMessage message = new ForumMessage(sujetAlumni, autreAlumni, "Contenu");
        setId(message, 56L);
        when(messageRepository.findById(56L)).thenReturn(Optional.of(message));
        when(utilisateurCourant.utilisateur(authentication)).thenReturn(alumni);

        ResponseEntity<?> reponse = controller.supprimerMessage(56L, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
