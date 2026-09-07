package inpg.sn.lms.entreprise.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
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
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.EntrepriseRepository;
import inpg.sn.lms.entreprise.dto.ReponseDemandeRequest;
import inpg.sn.lms.entreprise.entity.DemandeEntreprise;
import inpg.sn.lms.entreprise.enums.EnumStatutDemandeEntreprise;
import inpg.sn.lms.entreprise.enums.EnumTypeDemandeEntreprise;
import inpg.sn.lms.entreprise.repository.DemandeEntrepriseRepository;
import inpg.sn.lms.entreprise.repository.MessageEntrepriseRepository;
import inpg.sn.lms.espace_etudiant.service.UtilisateurCourantService;

/**
 * Volet pédagogique de l'espace Entreprise : réponse à une demande, et
 * cloisonnement de la messagerie — impossible d'écrire dans le canal d'une
 * entreprise inexistante (identifiant erroné ou d'une autre nature).
 */
@ExtendWith(MockitoExtension.class)
class PedagogieEntrepriseControllerTest {

    @Mock
    private DemandeEntrepriseRepository demandeRepository;
    @Mock
    private MessageEntrepriseRepository messageRepository;
    @Mock
    private EntrepriseRepository entrepriseRepository;
    @Mock
    private inpg.sn.lms.entreprise.repository.FicheTechniqueModuleRepository ficheRepository;
    @Mock
    private inpg.sn.lms.entreprise.service.FicheTechniqueModuleService ficheService;
    @Mock
    private inpg.sn.lms.entreprise.service.EntrepriseNotificationService notificationService;
    @Mock
    private UtilisateurCourantService utilisateurCourant;
    @Mock
    private Authentication authentication;

    private PedagogieEntrepriseController controller;

    @BeforeEach
    void setUp() {
        controller = new PedagogieEntrepriseController(demandeRepository, messageRepository, entrepriseRepository,
                ficheRepository, ficheService, notificationService, utilisateurCourant);
    }

    @Test
    @DisplayName("Demande inexistante : 404")
    void repondre_a_une_demande_inexistante_est_refuse() {
        when(demandeRepository.findById(42L)).thenReturn(Optional.empty());

        ResponseEntity<?> reponse = controller.repondre(42L, new ReponseDemandeRequest("EN_COURS", "Réponse"));

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Statut invalide : 400, la demande n'est pas modifiée")
    void repondre_avec_statut_invalide_est_refuse() {
        User user = new User();
        user.setId(1L);
        Entreprise entreprise = new Entreprise(user, "Entreprise X", null, "Adresse", "77000000");
        DemandeEntreprise demande = new DemandeEntreprise(entreprise, EnumTypeDemandeEntreprise.BESOIN_FORMATION, null,
                "Description", null, null);
        demande.setId(5L);
        when(demandeRepository.findById(5L)).thenReturn(Optional.of(demande));

        ResponseEntity<?> reponse = controller.repondre(5L, new ReponseDemandeRequest("STATUT_INEXISTANT", null));

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(demande.getStatut()).isEqualTo(EnumStatutDemandeEntreprise.SOUMISE);
        verify(demandeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Réponse valide : statut et texte de réponse mis à jour, horodatés")
    void repondre_valide_met_a_jour_le_statut_et_la_reponse() {
        User user = new User();
        user.setId(1L);
        Entreprise entreprise = new Entreprise(user, "Entreprise X", null, "Adresse", "77000000");
        DemandeEntreprise demande = new DemandeEntreprise(entreprise, EnumTypeDemandeEntreprise.BESOIN_FORMATION, null,
                "Description", null, null);
        demande.setId(5L);
        when(demandeRepository.findById(5L)).thenReturn(Optional.of(demande));
        when(demandeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<?> reponse = controller.repondre(5L,
                new ReponseDemandeRequest("REPONDUE", "Voici notre réponse"));

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(demande.getStatut()).isEqualTo(EnumStatutDemandeEntreprise.REPONDUE);
        assertThat(demande.getReponseEquipe()).isEqualTo("Voici notre réponse");
        assertThat(demande.getDateReponse()).isNotNull();
    }

    @Test
    @DisplayName("Messagerie d'une entreprise inexistante : 404, pas de fuite")
    void messages_d_une_entreprise_inexistante_est_refuse() {
        when(entrepriseRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseEntity<?> reponse = controller.messages(99L);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Envoi d'un message vers une entreprise inexistante : 404, rien n'est enregistré")
    void envoyerMessage_vers_entreprise_inexistante_est_refuse() {
        when(entrepriseRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseEntity<?> reponse = controller.envoyerMessage(99L, Map.of("contenu", "Bonjour"), authentication);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(messageRepository, never()).save(any());
    }
}
