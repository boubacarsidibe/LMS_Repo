package inpg.sn.lms.espace_etudiant.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import inpg.sn.lms.auth.entity.Formateur;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.FormateurRepository;
import inpg.sn.lms.auth.service.FormateurService;
import inpg.sn.lms.espace_etudiant.entity.Cours;
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
 * Rapport des comptes formateurs (demande responsable pédagogique) : doit
 * rester téléchargeable même quand un formateur n'a aucun cours affecté —
 * c'est précisément le cas que ce rapport existe pour signaler (voir
 * CLAUDE.md §3, {@code cours_formateur} commande tout le cloisonnement).
 */
@ExtendWith(MockitoExtension.class)
class PedagogieControllerRapportFormateursTest {

    @Mock private FormationService formationService;
    @Mock private PromotionService promotionService;
    @Mock private CoursService coursService;
    @Mock private SupportService supportService;
    @Mock private FormateurRepository formateurRepository;
    @Mock private FormateurService formateurService;
    @Mock private FormateurSuppressionService formateurSuppressionService;
    @Mock private NotificationService notificationService;
    @Mock private PresenceRepository presenceRepository;
    @Mock private SeanceRepository seanceRepository;
    @Mock private ForumMentionRepository forumMentionRepository;
    @Mock private ForumMessageRepository forumMessageRepository;
    @Mock private ForumSujetRepository forumSujetRepository;
    @Mock private NoteModuleRepository noteModuleRepository;
    @Mock private RetourEvaluationRepository retourEvaluationRepository;
    @Mock private RapportFormateursPdfService rapportFormateursPdfService;

    @InjectMocks private PedagogieController controller;

    private Formateur formateur(Long id, String nom, String prenom, String username, String email) {
        Formateur f = new Formateur();
        f.setId(id);
        f.setNom(nom);
        f.setPrenom(prenom);
        f.setTelephone("+22177000" + id);
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        f.setUser(user);
        return f;
    }

    private Cours cours(String titre, Formateur... titulaires) {
        Cours c = new Cours();
        c.setTitre(titre);
        c.getFormateurs().addAll(List.of(titulaires));
        return c;
    }

    @Test
    void genere_un_pdf_valide_avec_un_formateur_sans_cours() {
        Formateur avecCours = formateur(1L, "Diop", "Awa", "adiop", "awa@example.com");
        Formateur sansCours = formateur(2L, "Sarr", "Modou", "msarr", "modou@example.com");
        when(formateurRepository.findAll()).thenReturn(List.of(avecCours, sansCours));
        when(coursService.getAll()).thenReturn(List.of(cours("Thermodynamique", avecCours)));
        when(rapportFormateursPdfService.genererRapport(org.mockito.ArgumentMatchers.any()))
                .thenReturn("%PDF-1.4 test".getBytes(StandardCharsets.ISO_8859_1));

        ResponseEntity<byte[]> reponse = controller.genererRapportFormateurs();

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(new String(reponse.getBody(), 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }

    @Test
    void le_rapport_transmis_au_service_pdf_compte_correctement_les_formateurs_sans_cours() {
        Formateur avecCours = formateur(1L, "Diop", "Awa", "adiop", "awa@example.com");
        Formateur sansCours1 = formateur(2L, "Sarr", "Modou", "msarr", "modou@example.com");
        Formateur sansCours2 = formateur(3L, "Ba", "Fatou", "fba", "fatou@example.com");
        when(formateurRepository.findAll()).thenReturn(List.of(avecCours, sansCours1, sansCours2));
        when(coursService.getAll()).thenReturn(List.of(cours("Thermodynamique", avecCours)));
        when(rapportFormateursPdfService.genererRapport(org.mockito.ArgumentMatchers.any()))
                .thenReturn("%PDF-1.4 test".getBytes(StandardCharsets.ISO_8859_1));

        controller.genererRapportFormateurs();

        org.mockito.ArgumentCaptor<inpg.sn.lms.espace_etudiant.dto.RapportFormateursDto> captor =
                org.mockito.ArgumentCaptor.forClass(inpg.sn.lms.espace_etudiant.dto.RapportFormateursDto.class);
        org.mockito.Mockito.verify(rapportFormateursPdfService).genererRapport(captor.capture());

        assertThat(captor.getValue().totalFormateurs()).isEqualTo(3);
        assertThat(captor.getValue().formateursSansCours()).isEqualTo(2);
        assertThat(captor.getValue().formateurs()).filteredOn(f -> f.coursEnseignes().isEmpty())
                .extracting(f -> f.prenom() + " " + f.nom())
                .containsExactlyInAnyOrder("Modou Sarr", "Fatou Ba");
    }
}
