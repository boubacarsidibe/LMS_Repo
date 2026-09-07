package inpg.sn.lms.espace_alumni.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import inpg.sn.lms.auth.entity.AlumniProfil;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.espace_alumni.dto.AlumniProfilDto;
import inpg.sn.lms.espace_alumni.dto.AlumniProfilUpdateRequest;
import inpg.sn.lms.espace_alumni.dto.EnqueteSuiviDto;
import inpg.sn.lms.espace_alumni.dto.ReponseEnqueteSuiviDto;
import inpg.sn.lms.espace_alumni.dto.ReponseEnqueteSuiviRequest;
import inpg.sn.lms.espace_alumni.dto.SituationProfessionnelleDto;
import inpg.sn.lms.espace_alumni.dto.SituationProfessionnelleRequest;
import inpg.sn.lms.espace_alumni.dto.AlumniMetriquesDto;
import inpg.sn.lms.espace_alumni.entity.EnqueteSuivi;
import inpg.sn.lms.espace_alumni.entity.ReponseEnqueteSuivi;
import inpg.sn.lms.espace_alumni.enums.EnumStatutEmploi;
import inpg.sn.lms.espace_alumni.repository.ConsultationDocumentPartageRepository;
import inpg.sn.lms.espace_alumni.repository.DocumentPartageRepository;
import inpg.sn.lms.espace_alumni.repository.EnqueteSuiviRepository;
import inpg.sn.lms.espace_alumni.repository.ReponseEnqueteSuiviRepository;
import inpg.sn.lms.espace_alumni.service.AlumniProfilService;
import inpg.sn.lms.espace_alumni.service.SituationProfessionnelleService;
import inpg.sn.lms.espace_etudiant.dto.ReleveDelivreDto;
import inpg.sn.lms.espace_etudiant.entity.PorteeForumSujet;
import inpg.sn.lms.espace_etudiant.entity.ReleveNotes;
import inpg.sn.lms.espace_etudiant.repository.ForumMessageRepository;
import inpg.sn.lms.espace_etudiant.repository.ForumSujetRepository;
import inpg.sn.lms.espace_etudiant.service.ReleveDelivranceService;
import inpg.sn.lms.espace_etudiant.service.UtilisateurCourantService;

/**
 * Espace Alumni : profil, historique professionnel, relevés délivrés.
 *
 * <p>Le contrôleur est protégé à double titre — {@code @PreAuthorize} de classe et
 * règle d'URL dans {@code SecurityConfig} — mais ces tests portent sur la logique
 * métier une fois l'accès accordé : un compte sans profil alumni associé (jeton
 * valide, ligne {@code alumni_profils} absente) doit obtenir 404, pas une exception.
 */
@ExtendWith(MockitoExtension.class)
class AlumniControllerTest {

    @Mock
    private UtilisateurCourantService utilisateurCourant;
    @Mock
    private AlumniProfilService alumniProfilService;
    @Mock
    private SituationProfessionnelleService situationService;
    @Mock
    private ReleveDelivranceService releveDelivranceService;
    @Mock
    private EnqueteSuiviRepository enqueteRepository;
    @Mock
    private ReponseEnqueteSuiviRepository reponseRepository;
    @Mock
    private ForumSujetRepository forumSujetRepository;
    @Mock
    private ForumMessageRepository forumMessageRepository;
    @Mock
    private DocumentPartageRepository documentPartageRepository;
    @Mock
    private ConsultationDocumentPartageRepository consultationRepository;
    @Mock
    private Authentication authentication;

    private AlumniController controller;

    @BeforeEach
    void setUp() {
        controller = new AlumniController(utilisateurCourant, alumniProfilService, situationService,
                releveDelivranceService, new inpg.sn.lms.espace_etudiant.service.RelevePdfService(),
                enqueteRepository, reponseRepository, forumSujetRepository,
                forumMessageRepository, documentPartageRepository, consultationRepository);
    }

    /**
     * En base, {@code AlumniProfil.id} est copié depuis {@code User.id} par
     * {@code @MapsId} au moment du flush — un mécanisme Hibernate, invisible sur un
     * objet construit à la main hors de toute session. Le fixer explicitement ici
     * reproduit ce que verrait le contrôleur une fois l'entité chargée depuis la base.
     */
    private AlumniProfil profil() {
        User user = new User("awa_ndoye", "hash", "awa.ndoye@inpg.sn");
        user.setId(42L);
        AlumniProfil profil = new AlumniProfil(user);
        profil.setId(42L);
        return profil;
    }

    @Test
    @DisplayName("Sans profil alumni associé au compte : 404, pas une exception")
    void monProfilSansFicheRenvoie404() {
        when(utilisateurCourant.alumniProfil(authentication)).thenReturn(null);

        ResponseEntity<?> reponse = controller.monProfil(authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Profil trouvé : la réponse est déléguée à AlumniProfilService#toDto")
    void monProfilRenvoieLeDto() {
        AlumniProfil profil = profil();
        AlumniProfilDto dto = new AlumniProfilDto(42L, "Ndoye", "Awa", "ETU-2026-001", "M1 Data Science",
                "Ingénieure", "Petrosen", "Pétrole & Gaz", true, LocalDate.now());
        when(utilisateurCourant.alumniProfil(authentication)).thenReturn(profil);
        when(alumniProfilService.toDto(profil)).thenReturn(dto);

        ResponseEntity<?> reponse = controller.monProfil(authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reponse.getBody()).isSameAs(dto);
    }

    @Test
    @DisplayName("Ajout d'une situation sans intitulé de poste : 400, rien n'est écrit")
    void ajouterSituationSansIntituleEstRefusee() {
        when(utilisateurCourant.alumniProfil(authentication)).thenReturn(profil());
        SituationProfessionnelleRequest requete =
                new SituationProfessionnelleRequest("  ", "Petrosen", LocalDate.now(), null, EnumStatutEmploi.EN_EMPLOI);

        ResponseEntity<?> reponse = controller.ajouterSituation(requete, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Date de fin antérieure à la date de début : 400")
    void ajouterSituationDateFinAvantDebutEstRefusee() {
        when(utilisateurCourant.alumniProfil(authentication)).thenReturn(profil());
        SituationProfessionnelleRequest requete = new SituationProfessionnelleRequest(
                "Ingénieure Procédés", "Petrosen", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 1, 1),
                EnumStatutEmploi.EN_EMPLOI);

        ResponseEntity<?> reponse = controller.ajouterSituation(requete, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Ajout valide : délégué au service, 201")
    void ajouterSituationValideEstCreee() {
        AlumniProfil profil = profil();
        when(utilisateurCourant.alumniProfil(authentication)).thenReturn(profil);
        SituationProfessionnelleRequest requete = new SituationProfessionnelleRequest(
                "Ingénieure Procédés", "Petrosen", LocalDate.of(2026, 1, 1), null, EnumStatutEmploi.EN_EMPLOI);
        SituationProfessionnelleDto dto = new SituationProfessionnelleDto(
                1L, "Ingénieure Procédés", "Petrosen", LocalDate.of(2026, 1, 1), null, EnumStatutEmploi.EN_EMPLOI);
        when(situationService.creer(eq(profil), any(SituationProfessionnelleRequest.class))).thenReturn(dto);

        ResponseEntity<?> reponse = controller.ajouterSituation(requete, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(reponse.getBody()).isSameAs(dto);
    }

    @Test
    @DisplayName("Suppression d'une situation qui n'appartient pas au profil connecté : 404")
    void supprimerSituationEtrangereEstRefusee() {
        when(utilisateurCourant.alumniProfil(authentication)).thenReturn(profil());
        when(situationService.supprimer(eq(5L), any(AlumniProfil.class))).thenReturn(false);

        ResponseEntity<?> reponse = controller.supprimerSituation(5L, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Mes relevés : le relevé délivré est correctement mappé, y compris ses lignes")
    void mesRelevesMappeLesLignes() {
        AlumniProfil profil = profil();
        when(utilisateurCourant.alumniProfil(authentication)).thenReturn(profil);

        ReleveNotes releve = new ReleveNotes();
        releve.setPeriode("Semestre 1 — 2026/2027");
        releve.setDelivrePar(ReleveDelivranceService.AUTORITE);
        releve.setMoyenneGenerale(15.5);
        releve.setNombreNotes(3);
        when(releveDelivranceService.deLEtudiant(42L)).thenReturn(List.of(releve));

        ResponseEntity<?> reponse = controller.mesReleves(authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        List<ReleveDelivreDto> corps = (List<ReleveDelivreDto>) reponse.getBody();
        assertThat(corps).hasSize(1);
        assertThat(corps.get(0).periode()).isEqualTo("Semestre 1 — 2026/2027");
        assertThat(corps.get(0).moyenneGenerale()).isEqualTo(15.5);
        assertThat(corps.get(0).nombreNotes()).isEqualTo(3);
        verify(releveDelivranceService).deLEtudiant(42L);
    }

    @Test
    @DisplayName("Enquêtes : dejaRepondu reflète uniquement la réponse de l'alumni connecté")
    void enquetesSignaleLaReponseDuSeulAlumniConnecte() {
        AlumniProfil profil = profil();
        when(utilisateurCourant.alumniProfil(authentication)).thenReturn(profil);
        EnqueteSuivi enquete = new EnqueteSuivi("Suivi 2026", "Impact carrière");
        enquete.setId(9L);
        when(enqueteRepository.findByActiveTrueOrderByDateCreationDesc()).thenReturn(List.of(enquete));
        when(reponseRepository.findByEnqueteIdAndAlumniProfilId(9L, 42L))
                .thenReturn(Optional.of(new ReponseEnqueteSuivi(enquete, profil, 4, 5, null)));

        ResponseEntity<?> reponse = controller.enquetes(authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        List<EnqueteSuiviDto> corps = (List<EnqueteSuiviDto>) reponse.getBody();
        assertThat(corps).hasSize(1);
        assertThat(corps.get(0).dejaRepondu()).isTrue();
        verify(reponseRepository).findByEnqueteIdAndAlumniProfilId(9L, 42L);
    }

    @Test
    @DisplayName("Répondre à une enquête inconnue ou clôturée : 404")
    void repondreEnqueteInconnueOuInactiveEstRefusee() {
        when(utilisateurCourant.alumniProfil(authentication)).thenReturn(profil());
        when(enqueteRepository.findById(9L)).thenReturn(Optional.empty());
        ReponseEnqueteSuiviRequest requete = new ReponseEnqueteSuiviRequest(4, 5, null);

        ResponseEntity<?> reponse = controller.repondreEnquete(9L, requete, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(reponseRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    @DisplayName("Réponse avec une note hors de 1 à 5 : 400, rien n'est enregistré")
    void repondreEnqueteAvecNoteHorsBornesEstRefusee() {
        when(utilisateurCourant.alumniProfil(authentication)).thenReturn(profil());
        EnqueteSuivi enquete = new EnqueteSuivi("Suivi 2026", null);
        enquete.setId(9L);
        when(enqueteRepository.findById(9L)).thenReturn(Optional.of(enquete));
        ReponseEnqueteSuiviRequest requete = new ReponseEnqueteSuiviRequest(6, 5, null);

        ResponseEntity<?> reponse = controller.repondreEnquete(9L, requete, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(reponseRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    @DisplayName("Répondre deux fois à la même enquête met à jour la réponse existante, n'en crée pas une seconde")
    void repondreDeuxFoisMetAJourLaReponseExistante() {
        AlumniProfil profil = profil();
        when(utilisateurCourant.alumniProfil(authentication)).thenReturn(profil);
        EnqueteSuivi enquete = new EnqueteSuivi("Suivi 2026", null);
        enquete.setId(9L);
        when(enqueteRepository.findById(9L)).thenReturn(Optional.of(enquete));
        ReponseEnqueteSuivi existante = new ReponseEnqueteSuivi(enquete, profil, 2, 2, "Ancien avis");
        existante.setId(77L);
        when(reponseRepository.findByEnqueteIdAndAlumniProfilId(9L, 42L)).thenReturn(Optional.of(existante));
        when(reponseRepository.save(any(ReponseEnqueteSuivi.class))).thenAnswer(inv -> inv.getArgument(0));
        ReponseEnqueteSuiviRequest requete = new ReponseEnqueteSuiviRequest(4, 5, "Nouvel avis");

        ResponseEntity<?> reponse = controller.repondreEnquete(9L, requete, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        ReponseEnqueteSuiviDto corps = (ReponseEnqueteSuiviDto) reponse.getBody();
        assertThat(corps.id()).isEqualTo(77L);
        assertThat(corps.noteApplicationCompetences()).isEqualTo(4);
        assertThat(corps.commentaire()).isEqualTo("Nouvel avis");
    }

    @Test
    @DisplayName("Sans profil alumni associé au compte, mes-metriques renvoie 404")
    void mesMetriquesSansFicheRenvoie404() {
        when(utilisateurCourant.alumniProfil(authentication)).thenReturn(null);

        ResponseEntity<?> reponse = controller.mesMetriques(authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Mes métriques : agrège relevé le plus récent, parcours pro, forum, documents et enquêtes")
    void mesMetriquesAgregeLesDonneesDuProfilConnecte() {
        AlumniProfil profil = profil();
        profil.setDateCreation(LocalDate.now().minusYears(3));
        profil.setPosteActuel("Ingénieure Procédés");
        when(utilisateurCourant.alumniProfil(authentication)).thenReturn(profil);
        AlumniProfilDto identite = new AlumniProfilDto(42L, "Ndoye", "Awa", "ETU-2026-001", "M1 Data Science",
                "Ingénieure Procédés", "Petrosen", "Pétrole & Gaz", true, profil.getDateCreation());
        when(alumniProfilService.toDto(profil)).thenReturn(identite);

        ReleveNotes releveRecent = new ReleveNotes();
        releveRecent.setPeriode("Semestre 2 — 2025/2026");
        releveRecent.setMoyenneGenerale(14.2);
        ReleveNotes releveAncien = new ReleveNotes();
        releveAncien.setPeriode("Semestre 1 — 2025/2026");
        releveAncien.setMoyenneGenerale(12.0);
        when(releveDelivranceService.deLEtudiant(42L)).thenReturn(List.of(releveRecent, releveAncien));

        when(situationService.lister(42L)).thenReturn(List.of(
                new SituationProfessionnelleDto(1L, "Ingénieure Procédés", "Petrosen", LocalDate.now(), null,
                        EnumStatutEmploi.EN_EMPLOI)));
        when(forumSujetRepository.countByAuteurIdAndPortee(42L, PorteeForumSujet.ALUMNI)).thenReturn(2L);
        when(forumMessageRepository.countByAuteurIdAndSujet_Portee(42L, PorteeForumSujet.ALUMNI)).thenReturn(5L);
        when(documentPartageRepository.countByAuteurId(42L)).thenReturn(1L);
        when(consultationRepository.countByLecteurId(42L)).thenReturn(4L);
        when(reponseRepository.countByAlumniProfilId(42L)).thenReturn(3L);
        EnqueteSuivi enqueteActive = new EnqueteSuivi("Suivi 2026", null);
        enqueteActive.setId(9L);
        when(enqueteRepository.findByActiveTrueOrderByDateCreationDesc()).thenReturn(List.of(enqueteActive));

        ResponseEntity<?> reponse = controller.mesMetriques(authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        AlumniMetriquesDto corps = (AlumniMetriquesDto) reponse.getBody();
        assertThat(corps.anneesDepuisDiplome()).isEqualTo(3);
        assertThat(corps.promotionLibelle()).isEqualTo("M1 Data Science");
        assertThat(corps.moyenneGeneraleDernierReleve()).isEqualTo(14.2);
        assertThat(corps.periodeDernierReleve()).isEqualTo("Semestre 2 — 2025/2026");
        assertThat(corps.nombreRelevesDelivres()).isEqualTo(2);
        assertThat(corps.nombrePostesParcours()).isEqualTo(1);
        assertThat(corps.posteActuelRenseigne()).isTrue();
        assertThat(corps.nombreSujetsForumAlumni()).isEqualTo(2L);
        assertThat(corps.nombreMessagesForumAlumni()).isEqualTo(5L);
        assertThat(corps.nombreDocumentsPartages()).isEqualTo(1L);
        assertThat(corps.nombreDocumentsConsultes()).isEqualTo(4L);
        assertThat(corps.nombreEnquetesRepondues()).isEqualTo(3L);
        assertThat(corps.nombreEnquetesActives()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Mes métriques sans aucun relevé délivré : moyenne et période restent nulles, pas zéro")
    void mesMetriquesSansReleveLaisseLaMoyenneNulle() {
        AlumniProfil profil = profil();
        when(utilisateurCourant.alumniProfil(authentication)).thenReturn(profil);
        when(alumniProfilService.toDto(profil)).thenReturn(new AlumniProfilDto(42L, "Ndoye", "Awa", "ETU-2026-001",
                "M1 Data Science", null, null, null, true, profil.getDateCreation()));
        when(releveDelivranceService.deLEtudiant(42L)).thenReturn(List.of());
        when(situationService.lister(42L)).thenReturn(List.of());
        when(enqueteRepository.findByActiveTrueOrderByDateCreationDesc()).thenReturn(List.of());

        ResponseEntity<?> reponse = controller.mesMetriques(authentication);

        AlumniMetriquesDto corps = (AlumniMetriquesDto) reponse.getBody();
        assertThat(corps.moyenneGeneraleDernierReleve()).isNull();
        assertThat(corps.periodeDernierReleve()).isNull();
        assertThat(corps.nombreRelevesDelivres()).isZero();
        assertThat(corps.posteActuelRenseigne()).isFalse();
    }
}
