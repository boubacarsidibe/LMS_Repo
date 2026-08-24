package inpg.sn.lms.accreditation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

import inpg.sn.lms.accreditation.dto.CritereAccreditationRequest;
import inpg.sn.lms.accreditation.dto.DecisionAccreditationRequest;
import inpg.sn.lms.accreditation.entity.CritereAccreditation;
import inpg.sn.lms.accreditation.entity.DemandeAccreditation;
import inpg.sn.lms.accreditation.enums.EnumStatutDemandeAccreditation;
import inpg.sn.lms.accreditation.repository.CritereAccreditationRepository;
import inpg.sn.lms.accreditation.repository.DemandeAccreditationRepository;
import inpg.sn.lms.accreditation.repository.PieceAccreditationRepository;
import inpg.sn.lms.accreditation.service.AccreditationNotificationService;
import inpg.sn.lms.accreditation.service.PieceAccreditationStorageService;
import inpg.sn.lms.auth.entity.EcoleFormation;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.espace_etudiant.service.UtilisateurCourantService;

/**
 * Espace évaluateur : le motif est obligatoire pour un rejet (jamais pour une
 * accréditation), et une décision ne peut porter que sur une demande non
 * encore tranchée — ce contrôleur est protégé par rôle en amont
 * ({@code SecurityConfig} + {@code @PreAuthorize}), ces tests couvrent la
 * logique métier une fois l'accès admis.
 */
@ExtendWith(MockitoExtension.class)
class EvaluateurAccreditationControllerTest {

    @Mock
    private DemandeAccreditationRepository demandeRepository;
    @Mock
    private PieceAccreditationRepository pieceRepository;
    @Mock
    private CritereAccreditationRepository critereRepository;
    @Mock
    private UtilisateurCourantService utilisateurCourant;
    @Mock
    private PieceAccreditationStorageService storage;
    @Mock
    private AccreditationNotificationService notificationService;
    @Mock
    private Authentication authentication;

    private EvaluateurAccreditationController controller;
    private User evaluateur;
    private User ecoleUser;
    private EcoleFormation ecole;

    @BeforeEach
    void setUp() {
        controller = new EvaluateurAccreditationController(demandeRepository, pieceRepository, critereRepository,
                utilisateurCourant, storage, notificationService);
        evaluateur = new User();
        evaluateur.setId(1L);
        ecoleUser = new User();
        ecoleUser.setId(2L);
        ecoleUser.setEmail("ecole@example.sn");
        ecole = new EcoleFormation(ecoleUser, "École X", "Adresse", "77000000", null);
        ecole.setId(2L);
    }

    @Test
    @DisplayName("Rejet sans motif : 400, la demande n'est pas modifiée")
    void rejeter_sans_motif_est_refuse() {
        ResponseEntity<?> reponse = controller.rejeter(5L, new DecisionAccreditationRequest(""), authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(demandeRepository, never()).save(any());
        verify(notificationService, never()).notifierRejet(any(), any(), any());
    }

    @Test
    @DisplayName("Rejet d'une demande déjà tranchée : 404")
    void rejeter_demande_deja_tranchee_est_introuvable() {
        DemandeAccreditation demande = new DemandeAccreditation(ecole);
        demande.setId(5L);
        demande.setStatut(EnumStatutDemandeAccreditation.ACCREDITE);
        when(demandeRepository.findById(5L)).thenReturn(Optional.of(demande));

        ResponseEntity<?> reponse = controller.rejeter(5L, new DecisionAccreditationRequest("Dossier incomplet"),
                authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(demandeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Rejet valide : statut REJETE, motif enregistré, école notifiée")
    void rejeter_valide_est_applique() {
        DemandeAccreditation demande = new DemandeAccreditation(ecole);
        demande.setId(5L);
        demande.setStatut(EnumStatutDemandeAccreditation.EN_COURS_EVALUATION);
        when(demandeRepository.findById(5L)).thenReturn(Optional.of(demande));
        when(utilisateurCourant.utilisateur(authentication)).thenReturn(evaluateur);
        when(demandeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(pieceRepository.findByDemandeId(5L)).thenReturn(List.of());

        ResponseEntity<?> reponse = controller.rejeter(5L, new DecisionAccreditationRequest("Dossier incomplet"),
                authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(demande.getStatut()).isEqualTo(EnumStatutDemandeAccreditation.REJETE);
        assertThat(demande.getMotifRejet()).isEqualTo("Dossier incomplet");
        verify(notificationService).notifierRejet("ecole@example.sn", "École X", "Dossier incomplet");
    }

    @Test
    @DisplayName("Prise en charge d'une demande qui n'est pas SOUMISE : 404")
    void prendre_en_charge_demande_non_soumise_est_refusee() {
        DemandeAccreditation demande = new DemandeAccreditation(ecole);
        demande.setId(5L);
        demande.setStatut(EnumStatutDemandeAccreditation.EN_COURS_EVALUATION);
        when(demandeRepository.findById(5L)).thenReturn(Optional.of(demande));

        ResponseEntity<?> reponse = controller.prendreEnCharge(5L, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(demandeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Accréditation valide : statut ACCREDITE, école notifiée")
    void accrediter_valide_est_applique() {
        DemandeAccreditation demande = new DemandeAccreditation(ecole);
        demande.setId(5L);
        demande.setStatut(EnumStatutDemandeAccreditation.EN_COURS_EVALUATION);
        when(demandeRepository.findById(5L)).thenReturn(Optional.of(demande));
        when(utilisateurCourant.utilisateur(authentication)).thenReturn(evaluateur);
        when(demandeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(pieceRepository.findByDemandeId(5L)).thenReturn(List.of());

        ResponseEntity<?> reponse = controller.accrediter(5L, null, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(demande.getStatut()).isEqualTo(EnumStatutDemandeAccreditation.ACCREDITE);
        verify(notificationService).notifierAccreditation("ecole@example.sn", "École X");
    }

    @Test
    @DisplayName("Création d'un critère sans libellé : 400")
    void creer_critere_sans_libelle_est_refuse() {
        ResponseEntity<?> reponse = controller.creerCritere(new CritereAccreditationRequest("  ", null));

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(critereRepository, never()).save(any());
    }

    @Test
    @DisplayName("Création d'un critère valide : 201, persisté")
    void creer_critere_valide_est_enregistre() {
        when(critereRepository.save(any())).thenAnswer(inv -> {
            CritereAccreditation c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        ResponseEntity<?> reponse = controller
                .creerCritere(new CritereAccreditationRequest("Corps enseignant qualifié", null));

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(critereRepository).save(any());
    }

    @Test
    @DisplayName("Suppression d'un critère inexistant : 404")
    void supprimer_critere_inexistant_est_introuvable() {
        when(critereRepository.findById(1L)).thenReturn(Optional.empty());

        ResponseEntity<?> reponse = controller.supprimerCritere(1L);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(critereRepository, never()).deleteById(any());
    }
}
