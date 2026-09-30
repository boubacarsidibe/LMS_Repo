package inpg.sn.lms.recrutementformateur.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import inpg.sn.lms.auth.entity.Formateur;
import inpg.sn.lms.auth.entity.Role;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.FormateurRepository;
import inpg.sn.lms.auth.repository.RoleRepository;
import inpg.sn.lms.auth.service.UserService;
import inpg.sn.lms.espace_etudiant.repository.FormationRepository;
import inpg.sn.lms.espace_etudiant.service.UtilisateurCourantService;
import inpg.sn.lms.recrutementformateur.dto.ChangementStatutRequest;
import inpg.sn.lms.recrutementformateur.dto.DecisionCandidatureFormateurRequest;
import inpg.sn.lms.recrutementformateur.dto.OffreFormateurRequest;
import inpg.sn.lms.recrutementformateur.entity.CandidatureFormateur;
import inpg.sn.lms.recrutementformateur.entity.OffreRecrutementFormateur;
import inpg.sn.lms.recrutementformateur.enums.EnumStatutCandidatureFormateur;
import inpg.sn.lms.recrutementformateur.enums.EnumStatutOffreFormateur;
import inpg.sn.lms.recrutementformateur.enums.EnumTypeContratFormateur;
import inpg.sn.lms.recrutementformateur.repository.CandidatureFormateurRepository;
import inpg.sn.lms.recrutementformateur.repository.CertificationCandidatureFormateurRepository;
import inpg.sn.lms.recrutementformateur.repository.ExperienceCandidatureFormateurRepository;
import inpg.sn.lms.recrutementformateur.repository.FormationCandidatureFormateurRepository;
import inpg.sn.lms.recrutementformateur.repository.LangueCandidatureFormateurRepository;
import inpg.sn.lms.recrutementformateur.repository.OffreRecrutementFormateurRepository;
import inpg.sn.lms.recrutementformateur.repository.PieceCandidatureFormateurRepository;
import inpg.sn.lms.recrutementformateur.service.RecrutementFormateurNotificationService;

/**
 * Volet responsable admission : cycle de vie des offres, avancement du suivi
 * et décision — dont la bascule du compte candidat en formateur au RETENU.
 */
@ExtendWith(MockitoExtension.class)
class AdminRecrutementFormateurControllerTest {

    @Mock private OffreRecrutementFormateurRepository offreRepository;
    @Mock private CandidatureFormateurRepository candidatureRepository;
    @Mock private PieceCandidatureFormateurRepository pieceRepository;
    @Mock private FormationCandidatureFormateurRepository formationCandidatureRepository;
    @Mock private ExperienceCandidatureFormateurRepository experienceRepository;
    @Mock private LangueCandidatureFormateurRepository langueRepository;
    @Mock private CertificationCandidatureFormateurRepository certificationRepository;
    @Mock private FormationRepository formationRepository;
    @Mock private FormateurRepository formateurRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private UserService userService;
    @Mock private UtilisateurCourantService utilisateurCourant;
    @Mock private RecrutementFormateurNotificationService notificationService;
    @Mock private Authentication authentication;

    private AdminRecrutementFormateurController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminRecrutementFormateurController(offreRepository, candidatureRepository, pieceRepository,
                formationCandidatureRepository, experienceRepository, langueRepository, certificationRepository,
                formationRepository, formateurRepository, roleRepository, userService, utilisateurCourant,
                notificationService);
    }

    private OffreRecrutementFormateur offre(EnumStatutOffreFormateur statut) {
        OffreRecrutementFormateur o = new OffreRecrutementFormateur();
        o.setId(1L);
        o.setIntitule("Formateur HSE");
        o.setTypeContrat(EnumTypeContratFormateur.CDD);
        o.setStatut(statut);
        return o;
    }

    private CandidatureFormateur candidature(EnumStatutCandidatureFormateur statut) {
        User candidat = new User();
        candidat.setId(7L);
        candidat.setEmail("c@c.sn");
        CandidatureFormateur c = new CandidatureFormateur(offre(EnumStatutOffreFormateur.PUBLIEE), candidat,
                "Sow", "Modou", "c@c.sn", "770123456");
        c.setId(5L);
        c.setStatut(statut);
        return c;
    }

    // ---------- Offres ----------

    @Test
    @DisplayName("Publication d'une offre brouillon : PUBLIEE + date de publication posée")
    void publier_offre_brouillon() {
        OffreRecrutementFormateur o = offre(EnumStatutOffreFormateur.BROUILLON);
        when(offreRepository.findById(1L)).thenReturn(Optional.of(o));
        when(offreRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(candidatureRepository.countByOffreId(1L)).thenReturn(0L);

        ResponseEntity<?> reponse = controller.publierOffre(1L);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(o.getStatut()).isEqualTo(EnumStatutOffreFormateur.PUBLIEE);
        assertThat(o.getDatePublication()).isNotNull();
    }

    @Test
    @DisplayName("Publication d'une offre clôturée : 409")
    void publier_offre_cloturee_est_refuse() {
        when(offreRepository.findById(1L)).thenReturn(Optional.of(offre(EnumStatutOffreFormateur.CLOTUREE)));

        ResponseEntity<?> reponse = controller.publierOffre(1L);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(offreRepository, never()).save(any());
    }

    @Test
    @DisplayName("Suppression d'une offre avec candidatures : 409")
    void supprimer_offre_avec_candidatures_est_refuse() {
        when(offreRepository.findById(1L)).thenReturn(Optional.of(offre(EnumStatutOffreFormateur.PUBLIEE)));
        when(candidatureRepository.countByOffreId(1L)).thenReturn(3L);

        ResponseEntity<?> reponse = controller.supprimerOffre(1L);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(offreRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Création d'offre sans intitulé : 400")
    void creer_offre_sans_intitule_est_refuse() {
        ResponseEntity<?> reponse = controller.creerOffre(
                new OffreFormateurRequest("  ", null, null, null, "CDD", null, 1, null, null), authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(offreRepository, never()).save(any());
    }

    // ---------- Suivi ----------

    @Test
    @DisplayName("Changement de statut : saut DOSSIER_RECU → TERMINE interdit (400)")
    void changer_statut_saut_interdit_est_refuse() {
        when(candidatureRepository.findById(5L))
                .thenReturn(Optional.of(candidature(EnumStatutCandidatureFormateur.DOSSIER_RECU)));

        ResponseEntity<?> reponse = controller.changerStatut(5L, new ChangementStatutRequest("TERMINE"));

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(candidatureRepository, never()).save(any());
    }

    @Test
    @DisplayName("Changement de statut valide : EN_COURS_DE_TRAITEMENT + candidat notifié")
    void changer_statut_valide_notifie() {
        CandidatureFormateur c = candidature(EnumStatutCandidatureFormateur.DOSSIER_RECU);
        when(candidatureRepository.findById(5L)).thenReturn(Optional.of(c));
        when(candidatureRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(pieceRepository.findByCandidatureId(5L)).thenReturn(java.util.List.of());

        ResponseEntity<?> reponse = controller.changerStatut(5L,
                new ChangementStatutRequest("EN_COURS_DE_TRAITEMENT"));

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(c.getStatut()).isEqualTo(EnumStatutCandidatureFormateur.EN_COURS_DE_TRAITEMENT);
        verify(notificationService).notifierPriseEnCharge(anyString(), anyString(), anyString());
    }

    // ---------- Décision ----------

    @Test
    @DisplayName("Décision NON_RETENU sans motif : 400")
    void decider_non_retenu_sans_motif_est_refuse() {
        when(candidatureRepository.findById(5L))
                .thenReturn(Optional.of(candidature(EnumStatutCandidatureFormateur.EN_COURS_DE_TRAITEMENT)));

        ResponseEntity<?> reponse = controller.decider(5L,
                new DecisionCandidatureFormateurRequest("NON_RETENU", "  "), authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(candidatureRepository, never()).save(any());
    }

    @Test
    @DisplayName("Décision RETENU : TERMINE, compte basculé en formateur, fiche créée, école notifiée")
    void decider_retenu_cree_le_formateur() {
        CandidatureFormateur c = candidature(EnumStatutCandidatureFormateur.EN_COURS_DE_TRAITEMENT);
        when(candidatureRepository.findById(5L)).thenReturn(Optional.of(c));
        when(candidatureRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(pieceRepository.findByCandidatureId(5L)).thenReturn(java.util.List.of());
        when(formateurRepository.findByTelephone("770123456")).thenReturn(Optional.empty());
        when(formateurRepository.findByUserId(7L)).thenReturn(Optional.empty());
        when(roleRepository.findByCode("ROLE_FORMATEUR"))
                .thenReturn(new Role(4, "ROLE_FORMATEUR", "Formateur", null));
        when(utilisateurCourant.utilisateur(authentication)).thenReturn(new User());

        ResponseEntity<?> reponse = controller.decider(5L,
                new DecisionCandidatureFormateurRequest("RETENU", null), authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(c.getStatut()).isEqualTo(EnumStatutCandidatureFormateur.TERMINE);
        assertThat(c.isFormateurCree()).isTrue();
        assertThat(c.getCandidat().getRoles()).extracting(Role::getCode).containsExactly("ROLE_FORMATEUR");
        verify(formateurRepository).save(any(Formateur.class));
        verify(notificationService).notifierRetenu(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Décision RETENU avec téléphone déjà pris : 400, rien n'est créé")
    void decider_retenu_telephone_deja_pris_est_refuse() {
        CandidatureFormateur c = candidature(EnumStatutCandidatureFormateur.EN_COURS_DE_TRAITEMENT);
        when(candidatureRepository.findById(5L)).thenReturn(Optional.of(c));
        when(formateurRepository.findByTelephone("770123456"))
                .thenReturn(Optional.of(new Formateur()));

        ResponseEntity<?> reponse = controller.decider(5L,
                new DecisionCandidatureFormateurRequest("RETENU", null), authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(formateurRepository, never()).save(any());
        verify(candidatureRepository, never()).save(any());
    }

    @Test
    @DisplayName("Décision sur une candidature ayant déjà donné un compte formateur : 409")
    void decider_sur_formateur_deja_cree_est_refuse() {
        CandidatureFormateur c = candidature(EnumStatutCandidatureFormateur.TERMINE);
        c.setFormateurCree(true);
        when(candidatureRepository.findById(5L)).thenReturn(Optional.of(c));

        ResponseEntity<?> reponse = controller.decider(5L,
                new DecisionCandidatureFormateurRequest("RETENU", null), authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(candidatureRepository, never()).save(any());
    }
}
