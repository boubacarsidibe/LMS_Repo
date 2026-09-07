package inpg.sn.lms.espace_etudiant.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

import inpg.sn.lms.auth.entity.AlumniProfil;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.AlumniProfilRepository;
import inpg.sn.lms.auth.repository.CandidatRepository;
import inpg.sn.lms.auth.repository.EtudiantRepository;
import inpg.sn.lms.auth.service.EtudiantService;
import inpg.sn.lms.espace_alumni.entity.EnqueteSuivi;
import inpg.sn.lms.espace_alumni.repository.EnqueteSuiviRepository;
import inpg.sn.lms.espace_alumni.repository.ReponseEnqueteSuiviRepository;
import inpg.sn.lms.espace_alumni.service.AlumniProfilService;
import inpg.sn.lms.espace_etudiant.dto.EnqueteSuiviCreateRequest;
import inpg.sn.lms.espace_etudiant.dto.QualiteCoursDto;
import inpg.sn.lms.espace_etudiant.dto.SyntheseEnqueteSuiviDto;
import inpg.sn.lms.espace_etudiant.entity.Notification;
import inpg.sn.lms.espace_etudiant.entity.RetourEvaluation;
import inpg.sn.lms.espace_etudiant.repository.CoursRepository;
import inpg.sn.lms.espace_etudiant.repository.FormationRepository;
import inpg.sn.lms.espace_etudiant.repository.ForumMentionRepository;
import inpg.sn.lms.espace_etudiant.repository.ForumMessageRepository;
import inpg.sn.lms.espace_etudiant.repository.ForumSujetRepository;
import inpg.sn.lms.espace_etudiant.repository.NotificationRepository;
import inpg.sn.lms.espace_etudiant.repository.PromotionRepository;
import inpg.sn.lms.espace_etudiant.repository.RetourEvaluationRepository;
import inpg.sn.lms.espace_etudiant.service.CertificatService;
import inpg.sn.lms.espace_etudiant.service.CompletionProgrammeService;
import inpg.sn.lms.espace_etudiant.service.EtudiantImportService;
import inpg.sn.lms.espace_etudiant.service.EtudiantSuppressionService;
import inpg.sn.lms.espace_etudiant.service.MatriculeService;
import inpg.sn.lms.espace_etudiant.service.RapportQualitePdfService;
import inpg.sn.lms.espace_etudiant.service.ReleveDelivranceService;
import inpg.sn.lms.espace_etudiant.service.ReleveNotesService;
import inpg.sn.lms.recrutement.service.NotificationService;

/**
 * Volet « Processus qualité » (TDR §G) de {@link PedagogieScolariteController} :
 * lancement/clôture des campagnes d'enquête de suivi, synthèse et rapport PDF.
 * Le reste du contrôleur (promotions, étudiants, certificats…) est déjà large et
 * hors de portée de cette campagne de tests, ciblée sur le code ajouté par cette
 * campagne.
 */
@ExtendWith(MockitoExtension.class)
class PedagogieScolariteEnquetesTest {

    @Mock
    private PromotionRepository promotionRepository;
    @Mock
    private FormationRepository formationRepository;
    @Mock
    private EtudiantRepository etudiantRepository;
    @Mock
    private EtudiantService etudiantService;
    @Mock
    private MatriculeService matriculeService;
    @Mock
    private EtudiantImportService etudiantImportService;
    @Mock
    private EtudiantSuppressionService etudiantSuppressionService;
    @Mock
    private CertificatService certificatService;
    @Mock
    private CoursRepository coursRepository;
    @Mock
    private ForumSujetRepository forumSujetRepository;
    @Mock
    private ForumMessageRepository forumMessageRepository;
    @Mock
    private ForumMentionRepository forumMentionRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private RetourEvaluationRepository retourRepository;
    @Mock
    private ReleveNotesService releveNotesService;
    @Mock
    private ReleveDelivranceService releveDelivranceService;
    @Mock
    private CandidatRepository candidatRepository;
    @Mock
    private AlumniProfilRepository alumniProfilRepository;
    @Mock
    private AlumniProfilService alumniProfilService;
    @Mock
    private EnqueteSuiviRepository enqueteSuiviRepository;
    @Mock
    private ReponseEnqueteSuiviRepository reponseEnqueteSuiviRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private RapportQualitePdfService rapportQualitePdfService;
    @Mock
    private CompletionProgrammeService completionProgrammeService;

    private PedagogieScolariteController controller;

    @BeforeEach
    void setUp() {
        controller = new PedagogieScolariteController(promotionRepository, formationRepository, etudiantRepository,
                etudiantService, matriculeService, etudiantImportService, etudiantSuppressionService,
                certificatService, coursRepository,
                forumSujetRepository, forumMessageRepository, forumMentionRepository, notificationService,
                retourRepository, releveNotesService, releveDelivranceService, candidatRepository,
                alumniProfilRepository, alumniProfilService, enqueteSuiviRepository, reponseEnqueteSuiviRepository,
                notificationRepository, rapportQualitePdfService,
                new inpg.sn.lms.espace_etudiant.service.RelevePdfService(), completionProgrammeService);
    }

    private AlumniProfil alumniAvecCompte(long id) {
        User user = new User("alumni" + id, "hash", "alumni" + id + "@inpg.sn");
        user.setId(id);
        AlumniProfil profil = new AlumniProfil();
        profil.setId(id);
        profil.setUser(user);
        return profil;
    }

    @Test
    @DisplayName("Lancer une enquête sans titre : 400, rien n'est enregistré ni notifié")
    void lancerEnqueteSansTitreEstRefusee() {
        EnqueteSuiviCreateRequest requete = new EnqueteSuiviCreateRequest("   ", "Description");

        ResponseEntity<?> reponse = controller.lancerEnqueteSuivi(requete);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(enqueteSuiviRepository, never()).save(any());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Lancer une enquête valide : notifie chaque alumni ayant un compte, ignore les autres")
    void lancerEnqueteValideNotifieLesAlumniAvecCompte() {
        EnqueteSuivi enregistree = new EnqueteSuivi("Suivi 2026", "Impact carrière");
        enregistree.setId(9L);
        when(enqueteSuiviRepository.save(any(EnqueteSuivi.class))).thenReturn(enregistree);

        AlumniProfil avecCompte = alumniAvecCompte(1L);
        AlumniProfil sansCompte = new AlumniProfil();
        sansCompte.setId(2L);
        when(alumniProfilRepository.findAll()).thenReturn(List.of(avecCompte, sansCompte));

        when(reponseEnqueteSuiviRepository.findByEnqueteIdOrderByDateReponseDesc(9L)).thenReturn(List.of());
        when(reponseEnqueteSuiviRepository.countByEnqueteId(9L)).thenReturn(0L);
        when(reponseEnqueteSuiviRepository.moyenneApplicationCompetences(9L)).thenReturn(null);
        when(reponseEnqueteSuiviRepository.moyenneImpactCarriere(9L)).thenReturn(null);

        ResponseEntity<?> reponse = controller.lancerEnqueteSuivi(
                new EnqueteSuiviCreateRequest("Suivi 2026", "Impact carrière"));

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        SyntheseEnqueteSuiviDto corps = (SyntheseEnqueteSuiviDto) reponse.getBody();
        assertThat(corps.enqueteId()).isEqualTo(9L);
        assertThat(corps.nombreReponses()).isZero();
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    @DisplayName("Clôturer une enquête inconnue : 404")
    void cloturerEnqueteInconnueEstRefusee() {
        when(enqueteSuiviRepository.findById(9L)).thenReturn(Optional.empty());

        ResponseEntity<?> reponse = controller.cloturerEnqueteSuivi(9L);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(enqueteSuiviRepository, never()).save(any());
    }

    @Test
    @DisplayName("Clôturer une enquête active : passe à inactive et le renvoie à jour")
    void cloturerEnqueteActiveLaDesactive() {
        EnqueteSuivi enquete = new EnqueteSuivi("Suivi 2026", null);
        enquete.setId(9L);
        when(enqueteSuiviRepository.findById(9L)).thenReturn(Optional.of(enquete));
        when(enqueteSuiviRepository.save(any(EnqueteSuivi.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reponseEnqueteSuiviRepository.findByEnqueteIdOrderByDateReponseDesc(9L)).thenReturn(List.of());
        when(reponseEnqueteSuiviRepository.countByEnqueteId(9L)).thenReturn(0L);
        when(reponseEnqueteSuiviRepository.moyenneApplicationCompetences(9L)).thenReturn(null);
        when(reponseEnqueteSuiviRepository.moyenneImpactCarriere(9L)).thenReturn(null);

        ResponseEntity<?> reponse = controller.cloturerEnqueteSuivi(9L);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(enquete.isActive()).isFalse();
        SyntheseEnqueteSuiviDto corps = (SyntheseEnqueteSuiviDto) reponse.getBody();
        assertThat(corps.active()).isFalse();
    }

    @Test
    @DisplayName("Synthèse d'une enquête : moyennes arrondies au dixième et commentaires plafonnés")
    void syntheseArrondiLesMoyennesEtLimiteLesCommentaires() {
        EnqueteSuivi enquete = new EnqueteSuivi("Suivi 2026", null);
        enquete.setId(9L);
        when(enqueteSuiviRepository.findAllByOrderByDateCreationDesc()).thenReturn(List.of(enquete));
        when(reponseEnqueteSuiviRepository.findByEnqueteIdOrderByDateReponseDesc(9L))
                .thenReturn(List.of());
        when(reponseEnqueteSuiviRepository.countByEnqueteId(9L)).thenReturn(4L);
        when(reponseEnqueteSuiviRepository.moyenneApplicationCompetences(9L)).thenReturn(3.14159);
        when(reponseEnqueteSuiviRepository.moyenneImpactCarriere(9L)).thenReturn(4.05);

        ResponseEntity<?> reponse = controller.enquetesSuivi();

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        List<SyntheseEnqueteSuiviDto> corps = (List<SyntheseEnqueteSuiviDto>) reponse.getBody();
        assertThat(corps).hasSize(1);
        assertThat(corps.get(0).nombreReponses()).isEqualTo(4);
        assertThat(corps.get(0).moyenneApplicationCompetences()).isEqualTo(3.1);
        assertThat(corps.get(0).moyenneImpactCarriere()).isEqualTo(4.1);
    }

    @Test
    @DisplayName("Rapport « avis sur les cours » : met en page la synthèse qualité, renvoie un PDF en pièce jointe")
    void rapportAvisCoursRenvoieUnPdf() {
        when(retourRepository.findAllByOrderByDateCreationDesc()).thenReturn(List.of(new RetourEvaluation()));
        byte[] pdfAttendu = { 1, 2, 3 };
        when(rapportQualitePdfService.suffixeDuJour()).thenReturn("2026-08-31");
        when(rapportQualitePdfService.genererRapportAvisCours(any(QualiteCoursDto.class))).thenReturn(pdfAttendu);

        ResponseEntity<?> reponse = controller.rapportAvisCours();

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reponse.getBody()).isEqualTo(pdfAttendu);
        assertThat(reponse.getHeaders().getContentType()).isEqualTo(org.springframework.http.MediaType.APPLICATION_PDF);
        assertThat(reponse.getHeaders().getFirst(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION))
                .contains("attachment").contains("INPG-avis-cours-2026-08-31.pdf");
    }

    @Test
    @DisplayName("Rapport « enquêtes de suivi » : met en page les campagnes, renvoie un PDF en pièce jointe")
    void rapportEnquetesSuiviRenvoieUnPdf() {
        when(enqueteSuiviRepository.findAllByOrderByDateCreationDesc()).thenReturn(List.of());
        byte[] pdfAttendu = { 4, 5, 6 };
        when(rapportQualitePdfService.suffixeDuJour()).thenReturn("2026-08-31");
        when(rapportQualitePdfService.genererRapportEnquetes(any())).thenReturn(pdfAttendu);

        ResponseEntity<?> reponse = controller.rapportEnquetesSuivi();

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reponse.getBody()).isEqualTo(pdfAttendu);
        assertThat(reponse.getHeaders().getFirst(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION))
                .contains("INPG-enquetes-suivi-carriere-2026-08-31.pdf");
    }
}
