package inpg.sn.lms.entreprise.controller;

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

import inpg.sn.lms.auth.entity.Entreprise;
import inpg.sn.lms.auth.entity.Role;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.EntrepriseRepository;
import inpg.sn.lms.auth.repository.RoleRepository;
import inpg.sn.lms.entreprise.dto.DemandeEntrepriseCreateRequest;
import inpg.sn.lms.entreprise.dto.EntrepriseProfilRequest;
import inpg.sn.lms.entreprise.entity.DemandeEntreprise;
import inpg.sn.lms.entreprise.enums.EnumTypeDemandeEntreprise;
import inpg.sn.lms.entreprise.repository.DemandeEntrepriseRepository;
import inpg.sn.lms.entreprise.repository.MessageEntrepriseRepository;
import inpg.sn.lms.entreprise.service.EntrepriseNotificationService;
import inpg.sn.lms.espace_etudiant.repository.FormationRepository;
import inpg.sn.lms.espace_etudiant.service.UtilisateurCourantService;

/**
 * Espace Entreprise : validation de la soumission, exclusivité d'espace (un
 * compte ne peut ouvrir qu'un seul espace à la fois) et cloisonnement — une
 * entreprise ne doit jamais pouvoir consulter les demandes ou les messages
 * d'une autre.
 */
@ExtendWith(MockitoExtension.class)
class EntrepriseControllerTest {

    @Mock
    private EntrepriseRepository entrepriseRepository;
    @Mock
    private DemandeEntrepriseRepository demandeRepository;
    @Mock
    private MessageEntrepriseRepository messageRepository;
    @Mock
    private FormationRepository formationRepository;
    @Mock
    private inpg.sn.lms.entreprise.repository.FicheTechniqueModuleRepository ficheRepository;
    @Mock
    private inpg.sn.lms.entreprise.service.FicheTechniqueModuleService ficheService;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UtilisateurCourantService utilisateurCourant;
    @Mock
    private EntrepriseNotificationService notificationService;
    @Mock
    private Authentication authentication;

    private EntrepriseController controller;
    private User user;

    @BeforeEach
    void setUp() {
        controller = new EntrepriseController(entrepriseRepository, demandeRepository, messageRepository,
                formationRepository, ficheRepository, ficheService, roleRepository, utilisateurCourant,
                notificationService);
        user = new User();
        user.setId(7L);
    }

    private DemandeEntrepriseCreateRequest requeteValide(String type) {
        return new DemandeEntrepriseCreateRequest(
                new EntrepriseProfilRequest("Entreprise X", "Industrie", "Adresse", "77000000"), type, null,
                "Un besoin de formation", null, null);
    }

    @Test
    @DisplayName("Coordonnées obligatoires manquantes : 400, rien n'est enregistré")
    void soumettre_sans_champs_obligatoires_est_refuse() {
        when(utilisateurCourant.utilisateur(authentication)).thenReturn(user);

        ResponseEntity<?> reponse = controller.soumettreDemande(
                new DemandeEntrepriseCreateRequest(new EntrepriseProfilRequest("", "Industrie", "Adresse", "77000000"),
                        "BESOIN_FORMATION", null, "Description", null, null),
                authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(demandeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Description manquante : 400, rien n'est enregistré")
    void soumettre_sans_description_est_refuse() {
        when(utilisateurCourant.utilisateur(authentication)).thenReturn(user);

        ResponseEntity<?> reponse = controller.soumettreDemande(
                new DemandeEntrepriseCreateRequest(new EntrepriseProfilRequest("Entreprise X", null, "Adresse", "77000000"),
                        "BESOIN_FORMATION", null, "  ", null, null),
                authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(demandeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Type de demande invalide : 400, rien n'est enregistré")
    void soumettre_avec_type_invalide_est_refuse() {
        when(utilisateurCourant.utilisateur(authentication)).thenReturn(user);

        ResponseEntity<?> reponse = controller.soumettreDemande(requeteValide("TYPE_INEXISTANT"), authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(demandeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Compte donnant déjà accès à un autre espace : 403, aucune demande créée")
    void soumettre_avec_role_exclusif_deja_present_est_refuse() {
        Role roleEtudiant = new Role(1, "ROLE_ETUDIANT", "Étudiant", null);
        user.addRole(roleEtudiant);
        when(utilisateurCourant.utilisateur(authentication)).thenReturn(user);

        ResponseEntity<?> reponse = controller.soumettreDemande(requeteValide("BESOIN_FORMATION"), authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(demandeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Soumission valide : demande créée, rôle ROLE_ENTREPRISE accordé")
    void soumettre_valide_cree_la_demande_et_accorde_le_role() {
        when(utilisateurCourant.utilisateur(authentication)).thenReturn(user);
        when(entrepriseRepository.findByUserId(7L)).thenReturn(Optional.empty());
        when(entrepriseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(demandeRepository.save(any())).thenAnswer(inv -> {
            DemandeEntreprise d = inv.getArgument(0);
            d.setId(1L);
            return d;
        });
        Role roleEntreprise = new Role(2, "ROLE_ENTREPRISE", "Entreprise", null);
        when(roleRepository.findByCode("ROLE_ENTREPRISE")).thenReturn(roleEntreprise);

        ResponseEntity<?> reponse = controller.soumettreDemande(requeteValide("DEVIS_SESSION"), authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(user.getRoles()).extracting(Role::getCode).containsExactly("ROLE_ENTREPRISE");
        verify(notificationService).notifierNouvelleDemande("Entreprise X", EnumTypeDemandeEntreprise.DEVIS_SESSION);
    }

    @Test
    @DisplayName("Aucun profil entreprise associé au compte : mes demandes renvoie une liste vide, pas d'erreur")
    void mesDemandes_sans_profil_renvoie_liste_vide() {
        when(utilisateurCourant.entreprise(authentication)).thenReturn(null);

        ResponseEntity<?> reponse = controller.mesDemandes(authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reponse.getBody()).isEqualTo(List.of());
        verify(demandeRepository, never()).findByEntrepriseIdOrderByDateSoumissionDesc(any());
    }

    @Test
    @DisplayName("Mes demandes n'interroge que les demandes de ma propre entreprise")
    void mesDemandes_est_cloisonnee_a_sa_propre_entreprise() {
        User moi = new User();
        moi.setId(7L);
        Entreprise monEntreprise = new Entreprise(moi, "Mon Entreprise", null, "Adresse", "77000000");
        monEntreprise.setId(7L);
        when(utilisateurCourant.entreprise(authentication)).thenReturn(monEntreprise);
        when(demandeRepository.findByEntrepriseIdOrderByDateSoumissionDesc(7L)).thenReturn(List.of());

        controller.mesDemandes(authentication);

        verify(demandeRepository).findByEntrepriseIdOrderByDateSoumissionDesc(7L);
        verify(demandeRepository, never()).findByEntrepriseIdOrderByDateSoumissionDesc(99L);
    }

    @Test
    @DisplayName("Aucun profil entreprise associé au compte : mes messages renvoie une liste vide")
    void mesMessages_sans_profil_renvoie_liste_vide() {
        when(utilisateurCourant.entreprise(authentication)).thenReturn(null);

        ResponseEntity<?> reponse = controller.mesMessages(authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reponse.getBody()).isEqualTo(List.of());
    }

    @Test
    @DisplayName("Envoi de message sans profil entreprise associé : 403, rien n'est enregistré")
    void envoyerMessage_sans_profil_est_refuse() {
        when(utilisateurCourant.entreprise(authentication)).thenReturn(null);
        when(utilisateurCourant.utilisateur(authentication)).thenReturn(user);

        ResponseEntity<?> reponse = controller.envoyerMessage(java.util.Map.of("contenu", "Bonjour"), authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("Message vide : 400, rien n'est enregistré")
    void envoyerMessage_vide_est_refuse() {
        Entreprise monEntreprise = new Entreprise(user, "Mon Entreprise", null, "Adresse", "77000000");
        when(utilisateurCourant.entreprise(authentication)).thenReturn(monEntreprise);
        when(utilisateurCourant.utilisateur(authentication)).thenReturn(user);

        ResponseEntity<?> reponse = controller.envoyerMessage(java.util.Map.of("contenu", "   "), authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(messageRepository, never()).save(any());
    }
}
