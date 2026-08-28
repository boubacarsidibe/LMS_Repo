package inpg.sn.lms.espace_etudiant.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.EtudiantRepository;
import inpg.sn.lms.auth.repository.FormateurRepository;
import inpg.sn.lms.auth.repository.UserRepository;
import inpg.sn.lms.espace_etudiant.service.CertificatPdfService;
import inpg.sn.lms.espace_etudiant.service.CertificatService;
import inpg.sn.lms.espace_etudiant.service.CoursService;
import inpg.sn.lms.espace_etudiant.service.FormationService;
import inpg.sn.lms.espace_etudiant.service.NoteModuleService;
import inpg.sn.lms.espace_etudiant.service.PerimetreFormateurService;
import inpg.sn.lms.espace_etudiant.service.PresenceService;
import inpg.sn.lms.espace_etudiant.service.ReleveNotesService;
import inpg.sn.lms.espace_etudiant.service.PromotionService;
import inpg.sn.lms.espace_etudiant.service.RetourEvaluationService;
import inpg.sn.lms.espace_etudiant.service.SeanceService;
import inpg.sn.lms.espace_etudiant.repository.ForumMessageRepository;
import inpg.sn.lms.espace_etudiant.repository.ForumSujetRepository;
import inpg.sn.lms.espace_etudiant.service.ForumVisibiliteService;
import inpg.sn.lms.espace_etudiant.service.SupportCoursStorageService;
import inpg.sn.lms.espace_etudiant.service.SupportService;
import inpg.sn.lms.recrutement.service.NotificationService;

/**
 * Mise à jour du profil étudiant : champs obligatoires, unicité du téléphone et
 * format de réponse. Le contrôleur est instancié directement — ces méthodes ne
 * dépendent que des repositories, pas du contexte Spring.
 */
@ExtendWith(MockitoExtension.class)
class EspaceEtudiantProfilTest {

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

    @Mock private ReleveNotesService releveNotesService;

    private EspaceEtudiantController controller;
    private Authentication authentication;
    private User user;
    private Etudiant etudiant;

    @BeforeEach
    void preparer() {
        // Le contrôleur regroupe tout l'espace étudiant : ses dépendances sont donc
        // nombreuses, mais seuls userRepository et etudiantRepository servent aux
        // tests de profil ci-dessous — les autres mocks ne sont jamais stubbés.
        controller = new EspaceEtudiantController(userRepository, etudiantRepository, formateurRepository,
                formationService, coursService, promotionService, supportService,
                forumSujetRepository, forumMessageRepository, certificatService, certificatPdfService,
                notificationService,
                retourEvaluationService, perimetreFormateurService, noteModuleService,
                releveNotesService, presenceService,
                seanceService, supportCoursStorageService, forumVisibilite, "uploads");

        user = new User();
        user.setId(12L);
        user.setUsername("awa_diallo");
        user.setEmail("awa.diallo@example.sn");

        etudiant = new Etudiant(user, "Diallo", "Awa", "770000001");
        etudiant.setId(12L);

        authentication = new UsernamePasswordAuthenticationToken("awa_diallo", "n/a", java.util.List.of());
        when(userRepository.findByUsername("awa_diallo")).thenReturn(user);
    }

    @Test
    void met_a_jour_l_identite_et_renvoie_le_profil() {
        when(etudiantRepository.findByUserId(12L)).thenReturn(Optional.of(etudiant));
        when(etudiantRepository.findByTelephone("770000002")).thenReturn(Optional.empty());

        ResponseEntity<?> reponse = controller.updateProfile(
                Map.of("nom", "Diallo", "prenom", "Awa Marie", "telephone", "770000002"), authentication);

        assertEquals(HttpStatus.OK, reponse.getStatusCode());
        verify(etudiantRepository).save(etudiant);
        assertEquals("Awa Marie", etudiant.getPrenom());
        assertEquals("770000002", etudiant.getTelephone());

        Map<?, ?> corps = assertInstanceOf(Map.class, reponse.getBody());
        assertEquals("awa.diallo@example.sn", corps.get("email"));
        Map<?, ?> fiche = assertInstanceOf(Map.class, corps.get("etudiant"));
        assertEquals("Awa Marie", fiche.get("prenom"));
    }

    /** Les valeurs sont nettoyées : un champ réduit à des espaces vaut absent. */
    @Test
    void refuse_les_champs_vides() {
        when(etudiantRepository.findByUserId(12L)).thenReturn(Optional.of(etudiant));

        ResponseEntity<?> reponse = controller.updateProfile(
                Map.of("nom", "   ", "prenom", "Awa", "telephone", "770000001"), authentication);

        assertEquals(HttpStatus.BAD_REQUEST, reponse.getStatusCode());
        verify(etudiantRepository, never()).save(any());
    }

    @Test
    void refuse_un_telephone_deja_pris_par_un_autre_etudiant() {
        Etudiant autre = new Etudiant(new User(), "Ndiaye", "Moussa", "770000009");
        autre.setId(99L);

        when(etudiantRepository.findByUserId(12L)).thenReturn(Optional.of(etudiant));
        when(etudiantRepository.findByTelephone("770000009")).thenReturn(Optional.of(autre));

        ResponseEntity<?> reponse = controller.updateProfile(
                Map.of("nom", "Diallo", "prenom", "Awa", "telephone", "770000009"), authentication);

        assertEquals(HttpStatus.CONFLICT, reponse.getStatusCode());
        verify(etudiantRepository, never()).save(any());
    }

    /** Conserver son propre numéro ne doit pas être vu comme un conflit. */
    @Test
    void accepte_de_conserver_son_propre_telephone() {
        when(etudiantRepository.findByUserId(12L)).thenReturn(Optional.of(etudiant));
        when(etudiantRepository.findByTelephone("770000001")).thenReturn(Optional.of(etudiant));

        ResponseEntity<?> reponse = controller.updateProfile(
                Map.of("nom", "Diallo", "prenom", "Awa", "telephone", "770000001"), authentication);

        assertEquals(HttpStatus.OK, reponse.getStatusCode());
        verify(etudiantRepository).save(etudiant);
    }

    @Test
    void refuse_un_compte_sans_fiche_etudiant() {
        when(etudiantRepository.findByUserId(12L)).thenReturn(Optional.empty());

        ResponseEntity<?> reponse = controller.updateProfile(
                Map.of("nom", "Diallo", "prenom", "Awa", "telephone", "770000001"), authentication);

        assertEquals(HttpStatus.NOT_FOUND, reponse.getStatusCode());
        verify(etudiantRepository, never()).save(any());
    }
}
