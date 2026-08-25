package inpg.sn.lms.espace_etudiant.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import inpg.sn.lms.auth.entity.Formateur;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.EtudiantRepository;
import inpg.sn.lms.auth.repository.FormateurRepository;
import inpg.sn.lms.auth.repository.UserRepository;
import inpg.sn.lms.espace_etudiant.entity.ForumSujet;
import inpg.sn.lms.espace_etudiant.repository.ForumMessageRepository;
import inpg.sn.lms.espace_etudiant.repository.ForumSujetRepository;
import inpg.sn.lms.espace_etudiant.service.CertificatPdfService;
import inpg.sn.lms.espace_etudiant.service.CertificatService;
import inpg.sn.lms.espace_etudiant.service.CoursService;
import inpg.sn.lms.espace_etudiant.service.EvenementService;
import inpg.sn.lms.espace_etudiant.service.FormationService;
import inpg.sn.lms.espace_etudiant.service.ForumVisibiliteService;
import inpg.sn.lms.espace_etudiant.service.NoteModuleService;
import inpg.sn.lms.espace_etudiant.service.PerimetreFormateurService;
import inpg.sn.lms.espace_etudiant.service.PresenceService;
import inpg.sn.lms.espace_etudiant.service.PromotionService;
import inpg.sn.lms.espace_etudiant.service.ReleveNotesService;
import inpg.sn.lms.espace_etudiant.service.RetourEvaluationService;
import inpg.sn.lms.espace_etudiant.service.SeanceService;
import inpg.sn.lms.espace_etudiant.service.SupportCoursStorageService;
import inpg.sn.lms.espace_etudiant.service.SupportService;
import inpg.sn.lms.recrutement.service.NotificationService;

/**
 * Corrige le défaut signalé au point 14 de LOG_BLOQUANT.md : les actions de
 * modération de ce contrôleur (répondre, épingler, résoudre, supprimer) ne
 * vérifiaient l'appartenance à un cours pour aucun des deux rôles. Elles
 * délèguent désormais à {@link ForumVisibiliteService#estVisible}, la même
 * règle déjà utilisée par {@code ForumController} et {@code listSujets} — un
 * seul endroit répond à « ce sujet est-il accessible au connecté ? ».
 */
@ExtendWith(MockitoExtension.class)
class EspaceEtudiantForumModerationTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private EtudiantRepository etudiantRepository;
    @Mock
    private FormateurRepository formateurRepository;
    @Mock
    private FormationService formationService;
    @Mock
    private CoursService coursService;
    @Mock
    private PromotionService promotionService;
    @Mock
    private SupportService supportService;
    @Mock
    private EvenementService evenementService;
    @Mock
    private ForumSujetRepository forumSujetRepository;
    @Mock
    private ForumMessageRepository forumMessageRepository;
    @Mock
    private CertificatService certificatService;
    @Mock
    private CertificatPdfService certificatPdfService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private RetourEvaluationService retourEvaluationService;
    @Mock
    private PerimetreFormateurService perimetreFormateurService;
    @Mock
    private NoteModuleService noteModuleService;
    @Mock
    private PresenceService presenceService;
    @Mock
    private SeanceService seanceService;
    @Mock
    private SupportCoursStorageService supportCoursStorageService;
    @Mock
    private ForumVisibiliteService forumVisibilite;
    @Mock
    private ReleveNotesService releveNotesService;

    private EspaceEtudiantController controller;
    private Authentication authentication;
    private User user;
    private ForumSujet sujet;

    @BeforeEach
    void preparer() {
        controller = new EspaceEtudiantController(userRepository, etudiantRepository, formateurRepository,
                formationService, coursService, promotionService, supportService, evenementService,
                forumSujetRepository, forumMessageRepository, certificatService, certificatPdfService,
                notificationService, retourEvaluationService, perimetreFormateurService, noteModuleService,
                releveNotesService, presenceService, seanceService, supportCoursStorageService, forumVisibilite,
                "uploads");

        user = new User();
        user.setId(12L);
        user.setUsername("awa_diallo");
        authentication = new UsernamePasswordAuthenticationToken("awa_diallo", "n/a", List.of());
        when(userRepository.findByUsername("awa_diallo")).thenReturn(user);

        sujet = new ForumSujet("Titre", null, user);
        sujet.setId(7L);
        when(forumSujetRepository.findById(7L)).thenReturn(Optional.of(sujet));
    }

    @Test
    @DisplayName("Répondre à un sujet hors du cursus de l'étudiant : 404, aucun message enregistré")
    void repondre_a_un_sujet_non_visible_est_refuse() {
        when(forumVisibilite.estVisible(sujet, authentication)).thenReturn(false);
        MockMultipartFile fichier = new MockMultipartFile("fichier", new byte[0]);

        ResponseEntity<?> reponse = controller.repondreAuSujet(7L, "Bonjour", false, null, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(forumMessageRepository, never()).save(any());
    }

    @Test
    @DisplayName("Répondre à un sujet visible : message enregistré")
    void repondre_a_un_sujet_visible_est_accepte() {
        when(forumVisibilite.estVisible(sujet, authentication)).thenReturn(true);
        when(forumMessageRepository.findBySujetIdOrderByDateCreationAsc(7L)).thenReturn(List.of());

        ResponseEntity<?> reponse = controller.repondreAuSujet(7L, "Bonjour", false, null, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(forumMessageRepository).save(any());
    }

    @Test
    @DisplayName("Épingler un sujet non visible : 404, rien de modifié")
    void epingler_un_sujet_non_visible_est_refuse() {
        Formateur formateur = new Formateur(user, "Camara", "Ousmane", "770000000");
        when(formateurRepository.findByUserId(12L)).thenReturn(Optional.of(formateur));
        when(forumVisibilite.estVisible(sujet, authentication)).thenReturn(false);

        ResponseEntity<?> reponse = controller.toggleUrgenceSujet(7L, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(forumSujetRepository, never()).save(any());
    }

    @Test
    @DisplayName("Marquer résolu un sujet non visible : 404")
    void resoudre_un_sujet_non_visible_est_refuse() {
        Formateur formateur = new Formateur(user, "Camara", "Ousmane", "770000000");
        when(formateurRepository.findByUserId(12L)).thenReturn(Optional.of(formateur));
        when(forumVisibilite.estVisible(sujet, authentication)).thenReturn(false);

        ResponseEntity<?> reponse = controller.marquerSujetResolu(7L, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(forumSujetRepository, never()).save(any());
    }

    @Test
    @DisplayName("Supprimer un sujet non visible : 404, rien supprimé")
    void supprimer_un_sujet_non_visible_est_refuse() {
        Formateur formateur = new Formateur(user, "Camara", "Ousmane", "770000000");
        when(formateurRepository.findByUserId(12L)).thenReturn(Optional.of(formateur));
        when(forumVisibilite.estVisible(sujet, authentication)).thenReturn(false);

        ResponseEntity<?> reponse = controller.supprimerSujet(7L, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(forumSujetRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Supprimer un sujet visible : autorisé")
    void supprimer_un_sujet_visible_est_accepte() {
        Formateur formateur = new Formateur(user, "Camara", "Ousmane", "770000000");
        when(formateurRepository.findByUserId(12L)).thenReturn(Optional.of(formateur));
        when(forumVisibilite.estVisible(sujet, authentication)).thenReturn(true);

        ResponseEntity<?> reponse = controller.supprimerSujet(7L, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(forumSujetRepository).deleteById(7L);
    }
}
