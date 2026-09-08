package inpg.sn.lms.recrutementformateur.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
import org.springframework.security.core.Authentication;

import inpg.sn.lms.auth.entity.Role;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.RoleRepository;
import inpg.sn.lms.espace_etudiant.service.UtilisateurCourantService;
import inpg.sn.lms.recrutement.service.FichierStockeService;
import inpg.sn.lms.recrutementformateur.dto.CandidatureFormateurCreateRequest;
import inpg.sn.lms.recrutementformateur.entity.CandidatureFormateur;
import inpg.sn.lms.recrutementformateur.entity.OffreRecrutementFormateur;
import inpg.sn.lms.recrutementformateur.enums.EnumStatutCandidatureFormateur;
import inpg.sn.lms.recrutementformateur.enums.EnumStatutOffreFormateur;
import inpg.sn.lms.recrutementformateur.enums.EnumTypeContratFormateur;
import inpg.sn.lms.recrutementformateur.repository.CandidatureFormateurRepository;
import inpg.sn.lms.recrutementformateur.repository.OffreRecrutementFormateurRepository;
import inpg.sn.lms.recrutementformateur.repository.PieceCandidatureFormateurRepository;
import inpg.sn.lms.recrutementformateur.service.RecrutementFormateurNotificationService;

/**
 * Espace candidat formateur : validation du dépôt, exclusivité d'espace,
 * candidature unique par offre et cloisonnement des dossiers.
 */
@ExtendWith(MockitoExtension.class)
class CandidatFormateurControllerTest {

    @Mock private OffreRecrutementFormateurRepository offreRepository;
    @Mock private CandidatureFormateurRepository candidatureRepository;
    @Mock private PieceCandidatureFormateurRepository pieceRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private FichierStockeService fichierStockeService;
    @Mock private UtilisateurCourantService utilisateurCourant;
    @Mock private RecrutementFormateurNotificationService notificationService;
    @Mock private Authentication authentication;

    private CandidatFormateurController controller;
    private User user;

    @BeforeEach
    void setUp() {
        controller = new CandidatFormateurController(offreRepository, candidatureRepository, pieceRepository,
                roleRepository, fichierStockeService, utilisateurCourant, notificationService);
        user = new User();
        user.setId(7L);
        user.setEmail("candidat.formateur@example.sn");
    }

    private OffreRecrutementFormateur offrePubliee() {
        OffreRecrutementFormateur o = new OffreRecrutementFormateur();
        o.setId(1L);
        o.setIntitule("Formateur Forage");
        o.setTypeContrat(EnumTypeContratFormateur.VACATAIRE);
        o.setStatut(EnumStatutOffreFormateur.PUBLIEE);
        return o;
    }

    private static CandidatureFormateurCreateRequest requete(Long offreId) {
        return new CandidatureFormateurCreateRequest(offreId, "Diallo", "Awa", "770000000",
                "Ingénieur pétrole", 8, "Lettre de motivation");
    }

    @Test
    @DisplayName("Dépôt sans offre : 400")
    void postuler_sans_offre_est_refuse() {
        when(utilisateurCourant.utilisateur(authentication)).thenReturn(user);

        ResponseEntity<?> reponse = controller.postuler(requete(null), authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(candidatureRepository, never()).save(any());
    }

    @Test
    @DisplayName("Dépôt sur une offre non publiée : 409")
    void postuler_offre_non_publiee_est_refuse() {
        when(utilisateurCourant.utilisateur(authentication)).thenReturn(user);
        OffreRecrutementFormateur o = offrePubliee();
        o.setStatut(EnumStatutOffreFormateur.BROUILLON);
        when(offreRepository.findById(1L)).thenReturn(Optional.of(o));

        ResponseEntity<?> reponse = controller.postuler(requete(1L), authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(candidatureRepository, never()).save(any());
    }

    @Test
    @DisplayName("Compte donnant déjà accès à un autre espace : 403")
    void postuler_avec_role_ouvrant_est_refuse() {
        user.addRole(new Role(1, "ROLE_ETUDIANT", "Étudiant", null));
        when(utilisateurCourant.utilisateur(authentication)).thenReturn(user);
        when(offreRepository.findById(1L)).thenReturn(Optional.of(offrePubliee()));

        ResponseEntity<?> reponse = controller.postuler(requete(1L), authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(candidatureRepository, never()).save(any());
    }

    @Test
    @DisplayName("Candidature déjà en cours sur cette offre : 409")
    void postuler_avec_candidature_active_est_refuse() {
        when(utilisateurCourant.utilisateur(authentication)).thenReturn(user);
        when(offreRepository.findById(1L)).thenReturn(Optional.of(offrePubliee()));
        when(candidatureRepository.findByOffreIdAndCandidatIdAndStatutNot(1L, 7L,
                EnumStatutCandidatureFormateur.TERMINE))
                .thenReturn(List.of(new CandidatureFormateur()));

        ResponseEntity<?> reponse = controller.postuler(requete(1L), authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(candidatureRepository, never()).save(any());
    }

    @Test
    @DisplayName("Dépôt valide : 201, rôle ROLE_CANDIDAT_FORMATEUR accordé, école notifiée")
    void postuler_valide_cree_le_role_et_la_candidature() {
        when(utilisateurCourant.utilisateur(authentication)).thenReturn(user);
        when(offreRepository.findById(1L)).thenReturn(Optional.of(offrePubliee()));
        when(candidatureRepository.findByOffreIdAndCandidatIdAndStatutNot(any(), any(), any())).thenReturn(List.of());
        when(candidatureRepository.save(any())).thenAnswer(inv -> {
            CandidatureFormateur c = inv.getArgument(0);
            c.setId(5L);
            return c;
        });
        when(pieceRepository.findByCandidatureId(5L)).thenReturn(List.of());
        when(roleRepository.findByCode("ROLE_CANDIDAT_FORMATEUR"))
                .thenReturn(new Role(9, "ROLE_CANDIDAT_FORMATEUR", "Candidat formateur", null));

        ResponseEntity<?> reponse = controller.postuler(requete(1L), authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(user.getRoles()).extracting(Role::getCode).containsExactly("ROLE_CANDIDAT_FORMATEUR");
        verify(notificationService).notifierNouvelleCandidature("Formateur Forage");
        verify(notificationService).notifierDossierRecu(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Dépôt d'une pièce sur une candidature close : 409")
    void televerser_piece_sur_candidature_terminee_est_refuse() {
        when(utilisateurCourant.utilisateur(authentication)).thenReturn(user);
        CandidatureFormateur c = new CandidatureFormateur(offrePubliee(), user, "Diallo", "Awa",
                "a@a.sn", "770000000");
        c.setId(5L);
        c.setStatut(EnumStatutCandidatureFormateur.TERMINE);
        when(candidatureRepository.findById(5L)).thenReturn(Optional.of(c));

        ResponseEntity<?> reponse = controller.televerserPiece(5L, "CV", null, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(pieceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Consultation de la candidature d'un autre : 404")
    void candidature_d_autrui_est_invisible() {
        when(utilisateurCourant.utilisateur(authentication)).thenReturn(user);
        User autre = new User();
        autre.setId(99L);
        CandidatureFormateur c = new CandidatureFormateur(offrePubliee(), autre, "X", "Y", "x@y.sn", "770000001");
        c.setId(5L);
        when(candidatureRepository.findById(5L)).thenReturn(Optional.of(c));

        ResponseEntity<?> reponse = controller.candidature(5L, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
