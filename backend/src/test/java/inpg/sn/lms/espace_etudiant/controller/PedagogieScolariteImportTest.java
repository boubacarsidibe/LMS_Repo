package inpg.sn.lms.espace_etudiant.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import inpg.sn.lms.auth.dto.EtudiantDto;
import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.auth.repository.AlumniProfilRepository;
import inpg.sn.lms.auth.repository.CandidatRepository;
import inpg.sn.lms.auth.repository.EtudiantRepository;
import inpg.sn.lms.auth.service.EtudiantService;
import inpg.sn.lms.espace_alumni.repository.EnqueteSuiviRepository;
import inpg.sn.lms.espace_alumni.repository.ReponseEnqueteSuiviRepository;
import inpg.sn.lms.espace_alumni.service.AlumniProfilService;
import inpg.sn.lms.espace_etudiant.entity.Certificat;
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
 * {@code POST /api/pedagogie/etudiants/import}.
 *
 * <p>Signalé en recette : un import renvoyait 500 dès qu'une ligne omettait le
 * téléphone. La colonne {@code Etudiant.telephone} est {@code NOT NULL UNIQUE} en
 * base — une ligne sans téléphone provoquait une violation de contrainte que le
 * try/catch par ligne rattrapait bien, mais qui avait déjà fait échouer toute la
 * transaction PostgreSQL partagée par l'ensemble du fichier : chaque ligne
 * suivante échouait à son tour (« current transaction is aborted »), et Spring,
 * trouvant la transaction marquée pour retour arrière au moment de la valider,
 * faisait échouer la requête entière en 500 — alors que le contrôleur avait
 * construit une réponse 200 normale listant les rejets.
 *
 * <p>Couvre : le téléphone est désormais requis comme les trois autres colonnes
 * (rejet propre, pas de tentative d'insertion) ; une ligne en échec n'empêche pas
 * les lignes suivantes d'être traitées (chaque ligne passe par {@link
 * EtudiantImportService}, dans sa propre transaction). Couvre aussi {@code POST
 * /api/pedagogie/etudiants} (création manuelle), qui partage le même défaut :
 * le téléphone n'y était pas non plus exigé en amont de la contrainte NOT NULL.
 */
@ExtendWith(MockitoExtension.class)
class PedagogieScolariteImportTest {

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
                new inpg.sn.lms.espace_etudiant.service.RelevePdfService(), completionProgrammeService,
                new inpg.sn.lms.espace_etudiant.service.CertificatPdfService("https://lms.inpg.sn"),
                org.mockito.Mockito.mock(inpg.sn.lms.espace_etudiant.service.ActiviteEtudiantService.class));
    }

    private Etudiant etudiantCree(long id) {
        Etudiant etudiant = new Etudiant();
        etudiant.setId(id);
        return etudiant;
    }

    @Test
    @DisplayName("Ligne sans téléphone : rejetée proprement, aucune tentative de création")
    void ligne_sans_telephone_est_rejetee() {
        String csv = "nom;prenom;email;telephone\nFall;Awa;awa.fall.test@inpg.sn;";

        ResponseEntity<?> reponse = controller.importerEtudiants(Map.of("csv", csv));

        assertThat(reponse.getStatusCode().value()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        Map<String, Object> corps = (Map<String, Object>) reponse.getBody();
        assertThat(corps.get("crees")).isEqualTo(0);
        @SuppressWarnings("unchecked")
        List<String> rejets = (List<String>) corps.get("rejets");
        assertThat(rejets).hasSize(1);
        assertThat(rejets.get(0)).contains("telephone");
        verify(etudiantImportService, never()).creerPourImport(any(), any());
    }

    @Test
    @DisplayName("Une ligne en échec n'empêche pas les suivantes d'être traitées")
    void ligne_en_echec_n_empeche_pas_les_suivantes() {
        String csv = "nom;prenom;email;telephone\n"
                + "Diallo;Fatou;fatou.diallo.test@inpg.sn;771234567\n"
                + "Ndiaye;Moussa;moussa.ndiaye.test@inpg.sn;772345678";
        when(etudiantImportService.creerPourImport(any(EtudiantDto.class), isNull()))
                .thenThrow(new IllegalStateException("Cet email est déjà utilisé."))
                .thenReturn(etudiantCree(1L));

        ResponseEntity<?> reponse = controller.importerEtudiants(Map.of("csv", csv));

        assertThat(reponse.getStatusCode().value()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        Map<String, Object> corps = (Map<String, Object>) reponse.getBody();
        assertThat(corps.get("crees")).isEqualTo(1);
        @SuppressWarnings("unchecked")
        List<String> rejets = (List<String>) corps.get("rejets");
        assertThat(rejets).hasSize(1);
        verify(etudiantImportService, times(2)).creerPourImport(any(EtudiantDto.class), isNull());
        verify(notificationService, times(1)).envoyerEmail(eq("moussa.ndiaye.test@inpg.sn"), anyString(),
                anyString());
    }

    @Test
    @DisplayName("Ligne valide : créée, identifiants envoyés par courriel")
    void ligne_valide_est_creee() {
        String csv = "Sarr;Aminata;aminata.sarr.test@inpg.sn;773456789";
        when(etudiantImportService.creerPourImport(any(EtudiantDto.class), isNull())).thenReturn(etudiantCree(2L));

        ResponseEntity<?> reponse = controller.importerEtudiants(Map.of("csv", csv));

        @SuppressWarnings("unchecked")
        Map<String, Object> corps = (Map<String, Object>) reponse.getBody();
        assertThat(corps.get("crees")).isEqualTo(1);
        verify(notificationService, times(1)).envoyerEmail(eq("aminata.sarr.test@inpg.sn"), anyString(), anyString());
    }

    private EtudiantDto etudiantDtoValide() {
        EtudiantDto dto = new EtudiantDto();
        dto.setNom("Diop");
        dto.setPrenom("Ousmane");
        dto.setEmail("ousmane.diop.test@inpg.sn");
        dto.setUsername("odiop");
        dto.setPassword("motdepasse");
        dto.setTelephone("776543210");
        return dto;
    }

    @Test
    @DisplayName("Création manuelle : téléphone manquant, 400, aucune création tentée")
    void creation_manuelle_sans_telephone_est_refusee() {
        EtudiantDto dto = etudiantDtoValide();
        dto.setTelephone(null);

        ResponseEntity<?> reponse = controller.creerEtudiant(dto);

        assertThat(reponse.getStatusCode().value()).isEqualTo(400);
        verify(etudiantService, never()).createEtudiant(any());
    }

    @Test
    @DisplayName("Création manuelle : requête complète acceptée")
    void creation_manuelle_avec_telephone_est_acceptee() {
        EtudiantDto dto = etudiantDtoValide();
        Etudiant etudiant = new Etudiant();
        etudiant.setId(3L);
        etudiant.setNom(dto.getNom());
        etudiant.setPrenom(dto.getPrenom());
        etudiant.setTelephone(dto.getTelephone());
        when(etudiantService.createEtudiant(dto)).thenReturn(etudiant);
        when(etudiantRepository.save(etudiant)).thenReturn(etudiant);

        ResponseEntity<?> reponse = controller.creerEtudiant(dto);

        assertThat(reponse.getStatusCode().value()).isEqualTo(201);
    }

    // ===================== Certificat au modèle INPG =====================

    private Etudiant etudiantPourCertificat() {
        Etudiant etudiant = etudiantCree(5L);
        when(etudiantRepository.findById(5L)).thenReturn(java.util.Optional.of(etudiant));
        when(completionProgrammeService.coursNonTermines(etudiant)).thenReturn(java.util.List.of());
        return etudiant;
    }

    @Test
    @DisplayName("La délivrance conserve civilité, naissance, période et code saisis, sans fixer la date de délivrance")
    void delivrance_conserve_les_champs_du_modele() {
        etudiantPourCertificat();
        when(certificatService.save(any(Certificat.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var reponse = controller.delivrerCertificat(new inpg.sn.lms.espace_etudiant.dto.CertificatCreateRequest(
                5L, " Comptage du gaz ", null, "Monsieur", java.time.LocalDate.of(1985, 8, 24),
                java.time.LocalDate.of(2026, 2, 2), java.time.LocalDate.of(2026, 2, 6), " CG1001260206/03 "));

        assertThat(reponse.getStatusCode().value()).isEqualTo(201);
        org.mockito.ArgumentCaptor<Certificat> saisi = org.mockito.ArgumentCaptor.forClass(Certificat.class);
        verify(certificatService).save(saisi.capture());
        Certificat certificat = saisi.getValue();
        assertThat(certificat.getIntituleFormation()).isEqualTo("Comptage du gaz");
        assertThat(certificat.getCivilite()).isEqualTo("Monsieur");
        assertThat(certificat.getDateNaissance()).isEqualTo(java.time.LocalDate.of(1985, 8, 24));
        assertThat(certificat.getDateDebut()).isEqualTo(java.time.LocalDate.of(2026, 2, 2));
        assertThat(certificat.getDateFin()).isEqualTo(java.time.LocalDate.of(2026, 2, 6));
        assertThat(certificat.getCodeCertificat()).isEqualTo("CG1001260206/03");
        assertThat(certificat.getDateEmission())
                .as("la date de délivrance n'est jamais saisie : CertificatService la fixe à la création")
                .isNull();
    }

    @Test
    @DisplayName("Une période dont la fin précède le début est refusée, rien n'est enregistré")
    void delivrance_refuse_une_periode_inversee() {
        var reponse = controller.delivrerCertificat(new inpg.sn.lms.espace_etudiant.dto.CertificatCreateRequest(
                5L, "Comptage du gaz", null, "Monsieur", null,
                java.time.LocalDate.of(2026, 2, 6), java.time.LocalDate.of(2026, 2, 2), null));

        assertThat(reponse.getStatusCode().value()).isEqualTo(400);
        verify(etudiantRepository, never()).findById(any());
        verify(certificatService, never()).save(any());
    }

    @Test
    @DisplayName("Une civilité inconnue est refusée")
    void delivrance_refuse_une_civilite_inconnue() {
        var reponse = controller.delivrerCertificat(new inpg.sn.lms.espace_etudiant.dto.CertificatCreateRequest(
                5L, "Comptage du gaz", null, "Seigneur", null, null, null, null));

        assertThat(reponse.getStatusCode().value()).isEqualTo(400);
        verify(etudiantRepository, never()).findById(any());
        verify(certificatService, never()).save(any());
    }

    @Test
    @DisplayName("Un intitulé ou un code plus long que la colonne est refusé en 400 explicite, pas en faux « déjà utilisé »")
    void delivrance_refuse_les_valeurs_trop_longues() {
        var intituleLong = controller.delivrerCertificat(new inpg.sn.lms.espace_etudiant.dto.CertificatCreateRequest(
                5L, "A".repeat(151), null, "Monsieur", null, null, null, null));
        var codeLong = controller.delivrerCertificat(new inpg.sn.lms.espace_etudiant.dto.CertificatCreateRequest(
                5L, "Comptage du gaz", null, "Monsieur", null, null, null, "B".repeat(61)));

        assertThat(intituleLong.getStatusCode().value()).isEqualTo(400);
        assertThat(codeLong.getStatusCode().value()).isEqualTo(400);
        verify(certificatService, never()).save(any());
    }

    @Test
    @DisplayName("Un certificat identique (même étudiant, formation et période) n'est pas délivré deux fois")
    void delivrance_refuse_un_doublon() {
        Etudiant etudiant = etudiantPourCertificat();
        Certificat existant = new Certificat();
        existant.setIntituleFormation("Comptage du gaz");
        existant.setDateDebut(java.time.LocalDate.of(2026, 2, 2));
        existant.setDateFin(java.time.LocalDate.of(2026, 2, 6));
        when(certificatService.getByEtudiant(etudiant.getId())).thenReturn(java.util.List.of(existant));

        var reponse = controller.delivrerCertificat(new inpg.sn.lms.espace_etudiant.dto.CertificatCreateRequest(
                5L, " comptage DU gaz ", null, "Monsieur", null,
                java.time.LocalDate.of(2026, 2, 2), java.time.LocalDate.of(2026, 2, 6), null));

        assertThat(reponse.getStatusCode().value()).isEqualTo(409);
        verify(certificatService, never()).save(any());
    }
}
