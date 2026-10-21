package inpg.sn.lms.rendezvous.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import inpg.sn.lms.auth.entity.Role;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.repository.UserRepository;
import inpg.sn.lms.espace_etudiant.entity.Notification;
import inpg.sn.lms.espace_etudiant.repository.NotificationRepository;
import inpg.sn.lms.espace_etudiant.service.UtilisateurCourantService;
import inpg.sn.lms.rendezvous.repository.CreneauDisponibiliteRepository;
import inpg.sn.lms.rendezvous.repository.RendezVousRepository;

/**
 * {@code notifier} pointait auparavant toujours vers {@code "/rendez-vous"}, une
 * route qui n'existe dans aucun espace (formateur, pédagogie, admission, étudiant,
 * école, entreprise, candidat ont chacun leur propre racine — même piège déjà
 * rencontré pour les réponses du forum et de la messagerie privée). Ce fichier fige
 * le lien attendu pour chaque rôle destinataire.
 */
@ExtendWith(MockitoExtension.class)
class RendezVousServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private CreneauDisponibiliteRepository creneauRepository;
    @Mock
    private RendezVousRepository rendezVousRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private UtilisateurCourantService utilisateurCourant;

    private RendezVousService service;

    private RendezVousService creerService() {
        return new RendezVousService(userRepository, creneauRepository, rendezVousRepository,
                notificationRepository, utilisateurCourant);
    }

    private User utilisateurAvecRole(String code) {
        User user = new User("un_utilisateur", "n/a", "un.utilisateur@inpg.sn");
        Role role = new Role();
        role.setCode(code);
        user.addRole(role);
        return user;
    }

    private String lienNotifie(String codeRole) {
        service = creerService();
        service.notifier(utilisateurAvecRole(codeRole), "message", Notification.RENDEZ_VOUS_DEMANDE);
        ArgumentCaptor<Notification> capture = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(capture.capture());
        return capture.getValue().getLien();
    }

    @Test
    @DisplayName("Formateur : lien vers son espace, pas /rendez-vous")
    void lien_formateur() {
        assertThat(lienNotifie("ROLE_FORMATEUR")).isEqualTo("/espace-etudiant/formateur/rendez-vous");
    }

    @Test
    @DisplayName("Responsable pédagogique : lien vers son espace")
    void lien_pedagogie() {
        assertThat(lienNotifie("ROLE_RESPONSABLE_PEDAGOGIQUE")).isEqualTo("/pedagogie/rendez-vous");
    }

    @Test
    @DisplayName("Responsable admission : lien vers son espace")
    void lien_admission() {
        assertThat(lienNotifie("ROLE_RESPONSABLE_ADMISSION")).isEqualTo("/responsable-admission/rendez-vous");
    }

    @Test
    @DisplayName("Étudiant : lien vers son espace")
    void lien_etudiant() {
        assertThat(lienNotifie("ROLE_ETUDIANT")).isEqualTo("/espace-etudiant/rendez-vous");
    }

    @Test
    @DisplayName("École partenaire : lien vers son espace")
    void lien_ecole() {
        assertThat(lienNotifie("ROLE_ECOLE_FORMATION")).isEqualTo("/accreditation/rendez-vous");
    }

    @Test
    @DisplayName("Entreprise : lien vers son espace")
    void lien_entreprise() {
        assertThat(lienNotifie("ROLE_ENTREPRISE")).isEqualTo("/espace-entreprise/rendez-vous");
    }

    @Test
    @DisplayName("estPersonnel reconnaît le formateur, pas un rôle externe")
    void est_personnel() {
        service = creerService();
        assertThat(service.estPersonnel(utilisateurAvecRole("ROLE_FORMATEUR"))).isTrue();
        assertThat(service.estPersonnel(utilisateurAvecRole("ROLE_ETUDIANT"))).isFalse();
    }

    @Test
    @DisplayName("estInterlocuteurExterne reconnaît pédagogie et admission, pas le formateur")
    void est_interlocuteur_externe() {
        service = creerService();
        assertThat(service.estInterlocuteurExterne(utilisateurAvecRole("ROLE_RESPONSABLE_PEDAGOGIQUE"))).isTrue();
        assertThat(service.estInterlocuteurExterne(utilisateurAvecRole("ROLE_RESPONSABLE_ADMISSION"))).isTrue();
        assertThat(service.estInterlocuteurExterne(utilisateurAvecRole("ROLE_FORMATEUR"))).isFalse();
    }

    @Test
    @DisplayName("listerInterlocuteursExternes interroge le dépôt sur pédagogie ET admission, jamais formateur")
    void liste_interlocuteurs_externes_interroge_les_deux_roles() {
        service = creerService();
        when(userRepository.findDistinctByRoles_CodeInOrderByUsernameAsc(any())).thenReturn(List.of());

        service.listerInterlocuteursExternes();

        ArgumentCaptor<List<String>> capture = ArgumentCaptor.forClass(List.class);
        verify(userRepository).findDistinctByRoles_CodeInOrderByUsernameAsc(capture.capture());
        assertThat(capture.getValue())
                .containsExactlyInAnyOrder("ROLE_RESPONSABLE_PEDAGOGIQUE", "ROLE_RESPONSABLE_ADMISSION")
                .doesNotContain("ROLE_FORMATEUR");
    }
}
