package inpg.sn.lms.espace_etudiant.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import inpg.sn.lms.auth.dto.FormateurDto;
import inpg.sn.lms.auth.entity.Formateur;
import inpg.sn.lms.auth.repository.FormateurRepository;
import inpg.sn.lms.auth.service.FormateurService;
import inpg.sn.lms.espace_etudiant.repository.ForumMentionRepository;
import inpg.sn.lms.espace_etudiant.repository.ForumMessageRepository;
import inpg.sn.lms.espace_etudiant.repository.ForumSujetRepository;
import inpg.sn.lms.espace_etudiant.repository.NoteModuleRepository;
import inpg.sn.lms.espace_etudiant.repository.PresenceRepository;
import inpg.sn.lms.espace_etudiant.repository.RetourEvaluationRepository;
import inpg.sn.lms.espace_etudiant.repository.SeanceRepository;
import inpg.sn.lms.espace_etudiant.service.CoursService;
import inpg.sn.lms.espace_etudiant.service.FormateurSuppressionService;
import inpg.sn.lms.espace_etudiant.service.FormationService;
import inpg.sn.lms.espace_etudiant.service.PromotionService;
import inpg.sn.lms.espace_etudiant.service.RapportFormateursPdfService;
import inpg.sn.lms.espace_etudiant.service.SupportService;
import inpg.sn.lms.recrutement.service.NotificationService;

/**
 * {@code POST /api/pedagogie/formateurs} : jusqu'ici seul le responsable
 * d'admission pouvait ouvrir un compte formateur — la pédagogie, qui affecte
 * ensuite les formateurs aux cours, ne le pouvait pas. Couvre le mot de passe
 * optionnel (généré + envoyé par courriel) et la validation des champs requis.
 */
@ExtendWith(MockitoExtension.class)
class PedagogieFormateursTest {

    @Mock
    private FormationService formationService;
    @Mock
    private PromotionService promotionService;
    @Mock
    private CoursService coursService;
    @Mock
    private SupportService supportService;
    @Mock
    private FormateurRepository formateurRepository;
    @Mock
    private FormateurService formateurService;
    @Mock
    private FormateurSuppressionService formateurSuppressionService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private PresenceRepository presenceRepository;
    @Mock
    private SeanceRepository seanceRepository;
    @Mock
    private ForumMentionRepository forumMentionRepository;
    @Mock
    private ForumMessageRepository forumMessageRepository;
    @Mock
    private ForumSujetRepository forumSujetRepository;
    @Mock
    private NoteModuleRepository noteModuleRepository;
    @Mock
    private RetourEvaluationRepository retourEvaluationRepository;
    @Mock
    private RapportFormateursPdfService rapportFormateursPdfService;

    private PedagogieController controller;

    @BeforeEach
    void setUp() {
        controller = new PedagogieController(formationService, promotionService, coursService, supportService,
                formateurRepository, formateurService, formateurSuppressionService, notificationService,
                presenceRepository, seanceRepository,
                forumMentionRepository, forumMessageRepository, forumSujetRepository, noteModuleRepository,
                retourEvaluationRepository, rapportFormateursPdfService);
    }

    private FormateurDto requeteValide(String password) {
        FormateurDto dto = new FormateurDto();
        dto.setNom("Ndiaye");
        dto.setPrenom("Fatou");
        dto.setEmail("fatou.ndiaye@inpg.sn");
        dto.setUsername("fndiaye");
        dto.setPassword(password);
        dto.setTelephone("770000000");
        return dto;
    }

    @Test
    @DisplayName("Champs requis manquants : 400, aucune création tentée")
    void champs_requis_manquants_est_refuse() {
        FormateurDto dto = new FormateurDto();
        dto.setNom("Ndiaye");
        // prenom, email, username, telephone manquants

        ResponseEntity<?> reponse = controller.creerFormateur(dto);

        assertThat(reponse.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    @DisplayName("Téléphone manquant : 400, aucune création tentée (Formateur.telephone est NOT NULL en base)")
    void telephone_manquant_est_refuse() {
        FormateurDto dto = requeteValide("motdepasse");
        dto.setTelephone(null);

        ResponseEntity<?> reponse = controller.creerFormateur(dto);

        assertThat(reponse.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    @DisplayName("Mot de passe vide : généré côté service, puis envoyé par courriel")
    void mot_de_passe_vide_declenche_l_envoi() {
        FormateurDto requete = requeteValide("");
        when(formateurService.createFormateur(requete)).thenAnswer(invocation -> {
            FormateurDto dto = invocation.getArgument(0);
            dto.setPassword("Genere123!");
            Formateur formateur = new Formateur();
            formateur.setId(1L);
            return formateur;
        });

        ResponseEntity<?> reponse = controller.creerFormateur(requete);

        assertThat(reponse.getStatusCode().value()).isEqualTo(201);
        verify(notificationService, times(1)).envoyerEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Mot de passe saisi à la main : jamais envoyé par courriel")
    void mot_de_passe_saisi_n_est_pas_envoye() {
        FormateurDto requete = requeteValide("MotDePasseChoisi1!");
        Formateur formateur = new Formateur();
        formateur.setId(2L);
        when(formateurService.createFormateur(requete)).thenReturn(formateur);

        controller.creerFormateur(requete);

        verify(notificationService, never()).envoyerEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Téléphone déjà pris : 400 avec un message explicite, aucune création tentée")
    void telephone_deja_pris_est_refuse_avant_ecriture() {
        FormateurDto requete = requeteValide("motdepasse");
        when(formateurRepository.findByTelephone("770000000")).thenReturn(Optional.of(new Formateur()));

        ResponseEntity<?> reponse = controller.creerFormateur(requete);

        assertThat(reponse.getStatusCode().value()).isEqualTo(400);
        assertThat(reponse.getBody().toString()).contains("téléphone");
        verifyNoInteractions(formateurService);
        verify(notificationService, never()).envoyerEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Compte déjà existant : le message du service remonte tel quel, en 400")
    void compte_deja_existant_remonte_le_message() {
        FormateurDto requete = requeteValide("motdepasse");
        when(formateurService.createFormateur(requete))
                .thenThrow(new IllegalStateException("Ce nom d'utilisateur est déjà pris."));

        ResponseEntity<?> reponse = controller.creerFormateur(requete);

        assertThat(reponse.getStatusCode().value()).isEqualTo(400);
        verify(notificationService, never()).envoyerEmail(anyString(), anyString(), anyString());
    }
}
