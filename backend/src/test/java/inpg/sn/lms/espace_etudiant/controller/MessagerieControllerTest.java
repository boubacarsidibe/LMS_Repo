package inpg.sn.lms.espace_etudiant.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.auth.entity.Formateur;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.EtudiantRepository;
import inpg.sn.lms.auth.repository.FormateurRepository;
import inpg.sn.lms.espace_etudiant.dto.ConversationCreateRequest;
import inpg.sn.lms.espace_etudiant.dto.MessagePriveCreateRequest;
import inpg.sn.lms.espace_etudiant.entity.Conversation;
import inpg.sn.lms.espace_etudiant.entity.Cours;
import inpg.sn.lms.espace_etudiant.entity.MessagePrive;
import inpg.sn.lms.espace_etudiant.repository.ConversationRepository;
import inpg.sn.lms.espace_etudiant.repository.MessagePriveRepository;
import inpg.sn.lms.espace_etudiant.repository.NotificationRepository;
import inpg.sn.lms.espace_etudiant.service.HabilitationCoursService;
import inpg.sn.lms.espace_etudiant.service.PerimetreFormateurService;
import inpg.sn.lms.espace_etudiant.service.UtilisateurCourantService;

/**
 * `MessagerieController` (messagerie privée apprenant ↔ formateur) est arrivé le
 * 2026-08-28 par un push d'équipe, sans passer par le protocole de revue habituel
 * de ce dépôt — donc sans aucun test. Contrairement à {@code EspaceEtudiantController},
 * il n'était couvert par aucun garde-fou structurel : {@code EspaceEtudiantGardeFousTest}
 * ne réfléchit que sur cette seule classe, par son nom, pas sur le paquet entier.
 *
 * <p>Ce fichier comble les deux trous : un garde-fou structurel propre à ce
 * contrôleur (chaque endpoint ouvre sa transaction — l'annotation de rôle, elle,
 * est posée une fois pour toutes au niveau de la classe), et des tests de
 * comportement sur le point qui compte le plus ici : qu'on ne puisse ni démarrer
 * une conversation avec quelqu'un hors de son périmètre, ni lire ou répondre à
 * une conversation dont on n'est pas participant.
 */
@ExtendWith(MockitoExtension.class)
class MessagerieControllerTest {

    private static final List<Class<? extends Annotation>> ANNOTATIONS_DE_ROUTE = List.of(
            GetMapping.class, PostMapping.class, PutMapping.class,
            PatchMapping.class, DeleteMapping.class, RequestMapping.class);

    private static boolean estUneRoute(Method methode) {
        return ANNOTATIONS_DE_ROUTE.stream().anyMatch(methode::isAnnotationPresent);
    }

    @Test
    @DisplayName("Le contrôle de rôle est posé une fois pour toutes au niveau de la classe")
    void le_controle_de_role_est_porte_par_la_classe() {
        assertThat(MessagerieController.class.isAnnotationPresent(PreAuthorize.class)).isTrue();
    }

    @Test
    @DisplayName("Chaque endpoint ouvre sa transaction (même défaut que EspaceEtudiantGardeFousTest, sur une classe différente)")
    void chaque_endpoint_ouvre_sa_transaction() {
        List<String> sansTransaction = Arrays.stream(MessagerieController.class.getDeclaredMethods())
                .filter(MessagerieControllerTest::estUneRoute)
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
    private ConversationRepository conversationRepository;
    @Mock
    private MessagePriveRepository messageRepository;
    @Mock
    private EtudiantRepository etudiantRepository;
    @Mock
    private FormateurRepository formateurRepository;
    @Mock
    private HabilitationCoursService habilitation;
    @Mock
    private PerimetreFormateurService perimetreFormateur;
    @Mock
    private UtilisateurCourantService utilisateurCourant;
    @Mock
    private NotificationRepository notificationRepository;

    private MessagerieController controller;
    private Authentication authentication;
    private User etudiantUser;
    private Etudiant etudiant;
    private User formateurAutoriseUser;
    private Formateur formateurAutorise;
    private User formateurEtrangerUser;
    private Formateur formateurEtranger;

    @BeforeEach
    void preparer() {
        controller = new MessagerieController(conversationRepository, messageRepository, etudiantRepository,
                formateurRepository, habilitation, perimetreFormateur, utilisateurCourant, notificationRepository);

        etudiantUser = new User();
        etudiantUser.setId(1L);
        etudiantUser.setUsername("awa_diallo");
        etudiant = new Etudiant(etudiantUser, "Diallo", "Awa", "770000001");
        authentication = new UsernamePasswordAuthenticationToken("awa_diallo", "n/a", List.of());

        formateurAutoriseUser = new User();
        formateurAutoriseUser.setId(2L);
        formateurAutorise = new Formateur(formateurAutoriseUser, "Camara", "Ousmane", "770000002");

        formateurEtrangerUser = new User();
        formateurEtrangerUser.setId(3L);
        formateurEtranger = new Formateur(formateurEtrangerUser, "Ndiaye", "Fatou", "770000003");

        Cours coursSuivi = new Cours();
        coursSuivi.setNom("Thermodynamique");
        coursSuivi.getFormateurs().add(formateurAutorise);
        coursDeLetudiant = coursSuivi;
    }

    private Cours coursDeLetudiant;

    /** Commun à tous les endpoints : résoudre l'utilisateur connecté. */
    private void connecteEnTantQuEtudiant() {
        when(utilisateurCourant.utilisateur(authentication)).thenReturn(etudiantUser);
    }

    /** Nécessaire uniquement aux tests qui recoupent le destinataire avec les cours de l'étudiant. */
    private void avecCoursAutorise() {
        when(utilisateurCourant.etudiant(authentication)).thenReturn(etudiant);
        when(habilitation.coursAutorises(etudiant)).thenReturn(List.of(coursDeLetudiant));
    }

    // ===================== Démarrer une conversation =====================

    @Test
    @DisplayName("Démarrer une conversation avec un formateur hors du cursus de l'étudiant : refusé, rien enregistré")
    void demarrer_conversation_avec_destinataire_non_contactable_est_refuse() {
        connecteEnTantQuEtudiant();
        avecCoursAutorise();
        ConversationCreateRequest requete = new ConversationCreateRequest(formateurEtrangerUser.getId(), "Bonjour");

        ResponseEntity<?> reponse = controller.demarrerConversation(requete, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(conversationRepository, never()).save(any());
        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("Démarrer une conversation avec un formateur de ses cours : acceptée")
    void demarrer_conversation_avec_destinataire_contactable_est_acceptee() {
        connecteEnTantQuEtudiant();
        avecCoursAutorise();
        ConversationCreateRequest requete = new ConversationCreateRequest(formateurAutoriseUser.getId(), "Bonjour");
        when(conversationRepository.findEntreParticipants(etudiantUser.getId(), formateurAutoriseUser.getId()))
                .thenReturn(Optional.empty());
        when(formateurRepository.findById(formateurAutoriseUser.getId())).thenReturn(Optional.of(formateurAutorise));
        Conversation conversation = new Conversation(etudiantUser, formateurAutoriseUser);
        when(conversationRepository.save(any())).thenReturn(conversation);
        when(messageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<?> reponse = controller.demarrerConversation(requete, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(messageRepository).save(any());
        verify(notificationRepository).save(any());
    }

    @Test
    @DisplayName("Démarrer une conversation sans destinataire ni message : rejetée avant tout accès aux dépôts")
    void demarrer_conversation_sans_destinataire_ou_contenu_est_rejetee() {
        connecteEnTantQuEtudiant();
        ResponseEntity<?> reponse = controller.demarrerConversation(new ConversationCreateRequest(null, ""), authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(conversationRepository, never()).save(any());
    }

    // ===================== Lire un fil =====================

    @Test
    @DisplayName("Lire une conversation dont on n'est pas participant : refusé")
    void fil_est_refuse_a_qui_n_est_pas_participant() {
        connecteEnTantQuEtudiant();
        Conversation conversationDautrui = new Conversation(formateurAutoriseUser, formateurEtrangerUser);
        when(conversationRepository.findById(9L)).thenReturn(Optional.of(conversationDautrui));

        ResponseEntity<?> reponse = controller.fil(9L, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Lire sa propre conversation : acceptée")
    void fil_est_accepte_au_participant() {
        connecteEnTantQuEtudiant();
        Conversation conversation = new Conversation(etudiantUser, formateurAutoriseUser);
        when(conversationRepository.findById(9L)).thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversationIdAndAuteurIdNotAndLuFalse(9L, etudiantUser.getId()))
                .thenReturn(List.of());
        when(messageRepository.findByConversationIdOrderByDateEnvoiAsc(9L)).thenReturn(List.of());

        ResponseEntity<?> reponse = controller.fil(9L, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ===================== Répondre =====================

    @Test
    @DisplayName("Répondre à une conversation dont on n'est pas participant : refusé, rien enregistré")
    void repondre_est_refuse_a_qui_n_est_pas_participant() {
        connecteEnTantQuEtudiant();
        Conversation conversationDautrui = new Conversation(formateurAutoriseUser, formateurEtrangerUser);
        when(conversationRepository.findById(9L)).thenReturn(Optional.of(conversationDautrui));

        ResponseEntity<?> reponse = controller.repondre(9L, new MessagePriveCreateRequest("Bonjour"), authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("Répondre à sa propre conversation : acceptée")
    void repondre_est_acceptee_au_participant() {
        connecteEnTantQuEtudiant();
        Conversation conversation = new Conversation(etudiantUser, formateurAutoriseUser);
        when(conversationRepository.findById(9L)).thenReturn(Optional.of(conversation));
        when(messageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<?> reponse = controller.repondre(9L, new MessagePriveCreateRequest("Bonjour"), authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(messageRepository).save(any());
        verify(notificationRepository).save(any());
    }

    @Test
    @DisplayName("Répondre avec un message vide : rejetée")
    void repondre_avec_contenu_vide_est_rejetee() {
        connecteEnTantQuEtudiant();
        Conversation conversation = new Conversation(etudiantUser, formateurAutoriseUser);
        when(conversationRepository.findById(9L)).thenReturn(Optional.of(conversation));

        ResponseEntity<?> reponse = controller.repondre(9L, new MessagePriveCreateRequest("   "), authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(messageRepository, never()).save(any());
    }
}
