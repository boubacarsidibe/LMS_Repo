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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;

import inpg.sn.lms.accreditation.dto.DemandeAccreditationCreateRequest;
import inpg.sn.lms.accreditation.entity.DemandeAccreditation;
import inpg.sn.lms.accreditation.entity.PieceAccreditation;
import inpg.sn.lms.accreditation.enums.EnumStatutDemandeAccreditation;
import inpg.sn.lms.accreditation.enums.EnumTypeDocumentAccreditation;
import inpg.sn.lms.accreditation.repository.DemandeAccreditationRepository;
import inpg.sn.lms.accreditation.repository.PieceAccreditationRepository;
import inpg.sn.lms.accreditation.service.AccreditationNotificationService;
import inpg.sn.lms.accreditation.service.PieceAccreditationStorageService;
import inpg.sn.lms.auth.entity.EcoleFormation;
import inpg.sn.lms.auth.entity.Role;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.EcoleFormationRepository;
import inpg.sn.lms.auth.repository.RoleRepository;
import inpg.sn.lms.espace_etudiant.service.UtilisateurCourantService;

/**
 * Espace École de formation : validation de la soumission, exclusivité d'espace
 * (un compte ne peut ouvrir qu'un seul espace à la fois) et cloisonnement — une
 * école ne doit jamais pouvoir consulter ou compléter la demande d'une autre.
 */
@ExtendWith(MockitoExtension.class)
class EcoleAccreditationControllerTest {

    @Mock
    private EcoleFormationRepository ecoleFormationRepository;
    @Mock
    private DemandeAccreditationRepository demandeRepository;
    @Mock
    private PieceAccreditationRepository pieceRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UtilisateurCourantService utilisateurCourant;
    @Mock
    private PieceAccreditationStorageService storage;
    @Mock
    private AccreditationNotificationService notificationService;
    @Mock
    private Authentication authentication;

    private EcoleAccreditationController controller;
    private User user;

    @BeforeEach
    void setUp() {
        controller = new EcoleAccreditationController(ecoleFormationRepository, demandeRepository, pieceRepository,
                roleRepository, utilisateurCourant, storage, notificationService);
        user = new User();
        user.setId(7L);
    }

    @Test
    @DisplayName("Champs obligatoires manquants : 400, rien n'est enregistré")
    void soumettre_sans_champs_obligatoires_est_refuse() {
        when(utilisateurCourant.utilisateur(authentication)).thenReturn(user);

        ResponseEntity<?> reponse = controller.soumettreDemande(
                new DemandeAccreditationCreateRequest("", "Adresse", "77000000", null), authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(demandeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Compte donnant déjà accès à un autre espace : 403, aucune demande créée")
    void soumettre_avec_role_exclusif_deja_present_est_refuse() {
        Role roleEtudiant = new Role(1, "ROLE_ETUDIANT", "Étudiant", null);
        user.addRole(roleEtudiant);
        when(utilisateurCourant.utilisateur(authentication)).thenReturn(user);

        ResponseEntity<?> reponse = controller.soumettreDemande(
                new DemandeAccreditationCreateRequest("École X", "Adresse", "77000000", null), authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(demandeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Une demande active existe déjà : 409, aucune nouvelle demande créée")
    void soumettre_avec_demande_active_existante_est_refuse() {
        when(utilisateurCourant.utilisateur(authentication)).thenReturn(user);
        EcoleFormation ecole = new EcoleFormation(user, "École X", "Adresse", "77000000", null);
        ecole.setId(7L);
        when(ecoleFormationRepository.findByUserId(7L)).thenReturn(Optional.of(ecole));
        DemandeAccreditation active = new DemandeAccreditation(ecole);
        active.setId(1L);
        when(demandeRepository.findByEcoleIdAndStatutIn(7L,
                List.of(EnumStatutDemandeAccreditation.SOUMISE, EnumStatutDemandeAccreditation.EN_COURS_EVALUATION)))
                .thenReturn(List.of(active));

        ResponseEntity<?> reponse = controller.soumettreDemande(
                new DemandeAccreditationCreateRequest("École X", "Adresse", "77000000", null), authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(demandeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Soumission valide : demande créée, rôle ROLE_ECOLE_FORMATION accordé")
    void soumettre_valide_cree_la_demande_et_accorde_le_role() {
        when(utilisateurCourant.utilisateur(authentication)).thenReturn(user);
        when(ecoleFormationRepository.findByUserId(7L)).thenReturn(Optional.empty());
        when(ecoleFormationRepository.save(any())).thenAnswer(inv -> {
            EcoleFormation e = inv.getArgument(0);
            e.setId(7L);
            return e;
        });
        when(demandeRepository.findByEcoleIdAndStatutIn(any(), any())).thenReturn(List.of());
        when(demandeRepository.save(any())).thenAnswer(inv -> {
            DemandeAccreditation d = inv.getArgument(0);
            d.setId(1L);
            return d;
        });
        when(pieceRepository.findByDemandeId(1L)).thenReturn(List.of());
        Role roleEcole = new Role(2, "ROLE_ECOLE_FORMATION", "École de Formation", null);
        when(roleRepository.findByCode("ROLE_ECOLE_FORMATION")).thenReturn(roleEcole);

        ResponseEntity<?> reponse = controller.soumettreDemande(
                new DemandeAccreditationCreateRequest("École X", "Adresse", "77000000", "https://ecole.example"),
                authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(user.getRoles()).extracting(Role::getCode).containsExactly("ROLE_ECOLE_FORMATION");
        verify(notificationService).notifierNouvelleDemande("École X");
    }

    @Test
    @DisplayName("Consultation de la demande d'une autre école : 404, pas de fuite")
    void demande_d_une_autre_ecole_est_invisible() {
        EcoleFormation monEcole = new EcoleFormation(user, "Mon École", "Adresse", "77000000", null);
        monEcole.setId(7L);
        when(utilisateurCourant.ecoleFormation(authentication)).thenReturn(monEcole);
        User autreUser = new User();
        autreUser.setId(99L);
        EcoleFormation autreEcole = new EcoleFormation(autreUser, "Autre École", "Adresse", "77000000", null);
        autreEcole.setId(99L);
        DemandeAccreditation demandeAutrui = new DemandeAccreditation(autreEcole);
        demandeAutrui.setId(5L);
        when(demandeRepository.findById(5L)).thenReturn(Optional.of(demandeAutrui));

        ResponseEntity<?> reponse = controller.demande(5L, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Type de document invalide lors du dépôt d'une pièce : 400")
    void televerser_piece_avec_type_invalide_est_refuse() {
        EcoleFormation monEcole = new EcoleFormation(user, "Mon École", "Adresse", "77000000", null);
        monEcole.setId(7L);
        when(utilisateurCourant.ecoleFormation(authentication)).thenReturn(monEcole);
        DemandeAccreditation demande = new DemandeAccreditation(monEcole);
        demande.setId(5L);
        when(demandeRepository.findById(5L)).thenReturn(Optional.of(demande));
        MockMultipartFile fichier = new MockMultipartFile("fichier", "cv.pdf", "application/pdf", new byte[] { 1 });

        ResponseEntity<?> reponse = controller.televerserPiece(5L, "TYPE_INEXISTANT", fichier, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(pieceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Dépôt d'une pièce sur une demande déjà tranchée : 409")
    void televerser_piece_sur_demande_tranchee_est_refuse() {
        EcoleFormation monEcole = new EcoleFormation(user, "Mon École", "Adresse", "77000000", null);
        monEcole.setId(7L);
        when(utilisateurCourant.ecoleFormation(authentication)).thenReturn(monEcole);
        DemandeAccreditation demande = new DemandeAccreditation(monEcole);
        demande.setId(5L);
        demande.setStatut(EnumStatutDemandeAccreditation.ACCREDITE);
        when(demandeRepository.findById(5L)).thenReturn(Optional.of(demande));
        MockMultipartFile fichier = new MockMultipartFile("fichier", "cv.pdf", "application/pdf", new byte[] { 1 });

        ResponseEntity<?> reponse = controller.televerserPiece(5L, "CURRICULUM", fichier, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(pieceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Dépôt d'une pièce valide : enregistrée via le service de stockage")
    void televerser_piece_valide_est_enregistree() throws Exception {
        EcoleFormation monEcole = new EcoleFormation(user, "Mon École", "Adresse", "77000000", null);
        monEcole.setId(7L);
        when(utilisateurCourant.ecoleFormation(authentication)).thenReturn(monEcole);
        DemandeAccreditation demande = new DemandeAccreditation(monEcole);
        demande.setId(5L);
        when(demandeRepository.findById(5L)).thenReturn(Optional.of(demande));
        MockMultipartFile fichier = new MockMultipartFile("fichier", "cv.pdf", "application/pdf", new byte[] { 1 });
        when(storage.enregistrer(fichier)).thenReturn(null);
        PieceAccreditation piece = new PieceAccreditation(EnumTypeDocumentAccreditation.CURRICULUM, demande, null);
        piece.setId(9L);
        when(pieceRepository.save(any())).thenReturn(piece);

        ResponseEntity<?> reponse = controller.televerserPiece(5L, "CURRICULUM", fichier, authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(pieceRepository).save(any());
    }
}
