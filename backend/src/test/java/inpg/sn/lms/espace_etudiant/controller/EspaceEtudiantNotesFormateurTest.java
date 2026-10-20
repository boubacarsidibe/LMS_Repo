package inpg.sn.lms.espace_etudiant.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.auth.entity.Formateur;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.espace_etudiant.entity.Promotion;
import inpg.sn.lms.auth.repository.EtudiantRepository;
import inpg.sn.lms.auth.repository.FormateurRepository;
import inpg.sn.lms.auth.repository.UserRepository;
import inpg.sn.lms.espace_etudiant.entity.Cours;
import inpg.sn.lms.espace_etudiant.entity.NoteModule;
import inpg.sn.lms.espace_etudiant.repository.ForumMessageRepository;
import inpg.sn.lms.espace_etudiant.repository.ForumSujetRepository;
import inpg.sn.lms.espace_etudiant.service.CertificatPdfService;
import inpg.sn.lms.espace_etudiant.service.CertificatService;
import inpg.sn.lms.espace_etudiant.service.CoursService;
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
 * Corrige la fuite signalée par l'utilisateur : {@code GET /formateur/etudiants/{id}/notes}
 * vérifiait seulement que l'étudiant appartient à une cohorte du formateur, jamais que la
 * note elle-même provient d'un cours qu'il enseigne — un formateur pouvait donc lire les
 * notes qu'un collègue avait mises sur un autre module du même étudiant.
 */
@ExtendWith(MockitoExtension.class)
class EspaceEtudiantNotesFormateurTest {

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
    @Mock
    private ReleveNotesService releveNotesService;
    @Mock
    private inpg.sn.lms.espace_etudiant.service.ActiviteEtudiantService activiteEtudiantService;

    private EspaceEtudiantController controller;
    private Authentication authentication;
    private Formateur formateur;
    private Etudiant etudiant;
    private Cours coursDuFormateur;
    private Cours coursDuCollegue;

    @BeforeEach
    void preparer() {
        controller = new EspaceEtudiantController(userRepository, etudiantRepository, formateurRepository,
                formationService, coursService, promotionService, supportService,
                forumSujetRepository, forumMessageRepository, certificatService, certificatPdfService,
                notificationService, retourEvaluationService, perimetreFormateurService, noteModuleService,
                releveNotesService, presenceService, seanceService, supportCoursStorageService, forumVisibilite,
                activiteEtudiantService, "uploads");

        User user = new User();
        user.setId(1L);
        user.setUsername("ousmane_camara");
        when(userRepository.findByUsername("ousmane_camara")).thenReturn(user);
        authentication = new UsernamePasswordAuthenticationToken("ousmane_camara", "n/a", List.of());

        formateur = new Formateur();
        formateur.setId(1L);
        when(formateurRepository.findByUserId(1L)).thenReturn(Optional.of(formateur));

        Promotion promotion = new Promotion();
        etudiant = new Etudiant();
        etudiant.setId(12L);
        etudiant.setPromotion(promotion);
        when(etudiantRepository.findById(12L)).thenReturn(Optional.of(etudiant));
        when(perimetreFormateurService.cohorteIdsDuFormateur(formateur)).thenReturn(List.of(999L));
        // Le champ id de Promotion n'a pas de setter public : on le force par réflexion,
        // même patron que les autres tests de ce module.
        setId(promotion, 999L);

        coursDuFormateur = new Cours();
        coursDuCollegue = new Cours();
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
    @DisplayName("Ne renvoie que les notes des cours enseignés par le formateur connecté")
    void filtre_les_notes_par_cours_enseigne() {
        NoteModule noteAMoi = new NoteModule();
        noteAMoi.setCours(coursDuFormateur);
        noteAMoi.setModule("Algorithmique");
        noteAMoi.setNote(15);
        noteAMoi.setDateEvaluation(LocalDate.now());
        noteAMoi.setFormateurNom("Camara Ousmane");

        NoteModule noteDuCollegue = new NoteModule();
        noteDuCollegue.setCours(coursDuCollegue);
        noteDuCollegue.setModule("Anglais");
        noteDuCollegue.setNote(8);
        noteDuCollegue.setDateEvaluation(LocalDate.now());
        noteDuCollegue.setFormateurNom("Autre Formateur");

        when(noteModuleService.getByEtudiantId(12L)).thenReturn(List.of(noteAMoi, noteDuCollegue));
        when(perimetreFormateurService.estFormateurDuCours(formateur, coursDuFormateur)).thenReturn(true);
        when(perimetreFormateurService.estFormateurDuCours(formateur, coursDuCollegue)).thenReturn(false);

        ResponseEntity<?> reponse = controller.listNotesEtudiant(12L, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<?> notes = (List<?>) reponse.getBody();
        assertThat(notes).hasSize(1);
    }

    /**
     * La fiche de suivi servait « 0 % » de progression et « Non renseignée » en dernière connexion à tous
     * les étudiants — deux valeurs écrites en dur —, alors que l'étudiant voyait de son côté sa vraie
     * progression : l'écran de celui qui décide qui relancer disait le contraire de celui de l'intéressé.
     */
    @Test
    @DisplayName("La fiche de suivi porte la progression et l'activité réelles de l'étudiant, pas des valeurs codées en dur")
    void la_fiche_de_suivi_porte_la_progression_et_l_activite_reelles() {
        etudiant.setPrenom("Fatou");
        etudiant.setNom("Ndiaye");
        when(activiteEtudiantService.progression(etudiant)).thenReturn(72);
        when(activiteEtudiantService.derniereActivite(etudiant)).thenReturn("19 oct. 2026");

        ResponseEntity<?> reponse = controller.getStudent(12L, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        var fiche = (inpg.sn.lms.espace_etudiant.dto.EtudiantSuiviDto) reponse.getBody();
        assertThat(fiche.progression()).isEqualTo(72);
        assertThat(fiche.derniereActivite()).isEqualTo("19 oct. 2026");
    }
}
