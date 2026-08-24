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

import inpg.sn.lms.auth.entity.AlumniProfil;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.espace_alumni.dto.AlumniProfilDto;
import inpg.sn.lms.espace_alumni.dto.AlumniProfilUpdateRequest;
import inpg.sn.lms.espace_alumni.dto.SituationProfessionnelleDto;
import inpg.sn.lms.espace_alumni.dto.SituationProfessionnelleRequest;
import inpg.sn.lms.espace_alumni.service.AlumniProfilService;
import inpg.sn.lms.espace_alumni.service.SituationProfessionnelleService;
import inpg.sn.lms.espace_etudiant.dto.ReleveDelivreDto;
import inpg.sn.lms.espace_etudiant.entity.ReleveNotes;
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
    private Authentication authentication;

    private AlumniController controller;

    @BeforeEach
    void setUp() {
        controller = new AlumniController(utilisateurCourant, alumniProfilService, situationService,
                releveDelivranceService);
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
                new SituationProfessionnelleRequest("  ", "Petrosen", LocalDate.now(), null);

        ResponseEntity<?> reponse = controller.ajouterSituation(requete, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Date de fin antérieure à la date de début : 400")
    void ajouterSituationDateFinAvantDebutEstRefusee() {
        when(utilisateurCourant.alumniProfil(authentication)).thenReturn(profil());
        SituationProfessionnelleRequest requete = new SituationProfessionnelleRequest(
                "Ingénieure Procédés", "Petrosen", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 1, 1));

        ResponseEntity<?> reponse = controller.ajouterSituation(requete, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Ajout valide : délégué au service, 201")
    void ajouterSituationValideEstCreee() {
        AlumniProfil profil = profil();
        when(utilisateurCourant.alumniProfil(authentication)).thenReturn(profil);
        SituationProfessionnelleRequest requete = new SituationProfessionnelleRequest(
                "Ingénieure Procédés", "Petrosen", LocalDate.of(2026, 1, 1), null);
        SituationProfessionnelleDto dto = new SituationProfessionnelleDto(
                1L, "Ingénieure Procédés", "Petrosen", LocalDate.of(2026, 1, 1), null);
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
}
