package inpg.sn.lms.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import inpg.sn.lms.auth.dto.AuthRequest;
import inpg.sn.lms.auth.entity.Role;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.enums.EnumTypeInscription;
import inpg.sn.lms.auth.service.FormateurService;
import inpg.sn.lms.auth.service.OtpService;
import inpg.sn.lms.auth.service.UserService;
import inpg.sn.lms.security.JwtUtils;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private OtpService otpService;
    @Mock
    private UserService userService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private FormateurService formateurService;

    private AuthController controller;

    @BeforeEach
    void initialiserControleur() {
        controller = new AuthController(authenticationManager, jwtUtils, otpService, userService,
                passwordEncoder, formateurService);
    }

    @Test
    @DisplayName("Un mot de passe invalide ne consomme pas le code OTP")
    void reset_refuse_le_mot_de_passe_avant_la_validation_otp() {
        var response = controller.resetPassword(Map.of(
                "email", "alice@example.test",
                "code", "123456",
                "newPassword", "court"));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        verifyNoInteractions(otpService, userService);
    }

    private static User compte(String username, EnumTypeInscription type, String... rolesMetier) {
        User user = new User(username, "hash", username + "@example.test");
        user.setId(7L);
        user.setTypeInscription(type);
        for (String code : rolesMetier) {
            Role role = new Role();
            role.setCode(code);
            user.addRole(role);
        }
        return user;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> carteUtilisateur(Object corps) {
        return (Map<String, Object>) ((Map<String, Object>) corps).get("user");
    }

    /**
     * Sans ce champ, le navigateur ne savait pas qu'une école inscrite mais n'ayant
     * encore rien soumis (aucun rôle métier) était une école : à la connexion elle
     * atterrissait sur l'accueil des candidats, sans rien qui mène à son espace.
     */
    @Test
    @DisplayName("La connexion renvoie le type d'inscription, même pour un compte sans rôle métier")
    void login_renvoie_le_type_inscription() {
        User ecole = compte("ecole_test", EnumTypeInscription.ECOLE);
        when(userService.findByEmail("ecole_test@example.test")).thenReturn(ecole);
        when(jwtUtils.generateToken(ecole)).thenReturn("jeton");

        var response = controller.login(new AuthRequest("ecole_test@example.test", "Motdepasse1"));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        Map<String, Object> carte = carteUtilisateur(response.getBody());
        assertThat(carte.get("typeInscription")).isEqualTo("ECOLE");
        assertThat((List<?>) carte.get("roles")).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("/current-user renvoie le type d'inscription : il doit survivre à un rechargement de page")
    void current_user_renvoie_le_type_inscription() {
        User entreprise = compte("entreprise_test", EnumTypeInscription.ENTREPRISE, "ROLE_ENTREPRISE");
        when(userService.findByUsername("entreprise_test")).thenReturn(entreprise);
        UserDetails details = org.springframework.security.core.userdetails.User
                .withUsername("entreprise_test").password("hash").authorities("ROLE_ENTREPRISE").build();

        var response = controller.getCurrentUser(details);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        Map<String, Object> carte = (Map<String, Object>) response.getBody();
        assertThat(carte.get("typeInscription")).isEqualTo("ENTREPRISE");
        assertThat((List<String>) carte.get("roles")).containsExactly("ROLE_ENTREPRISE");
    }

    @Test
    @DisplayName("Un compte sans type d'inscription est traité comme un candidat, valeur par défaut de la colonne")
    void un_type_absent_vaut_candidat() {
        User ancien = compte("ancien_test", null);
        when(userService.findByEmail("ancien_test@example.test")).thenReturn(ancien);
        when(jwtUtils.generateToken(ancien)).thenReturn("jeton");

        var response = controller.login(new AuthRequest("ancien_test@example.test", "Motdepasse1"));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(carteUtilisateur(response.getBody()).get("typeInscription")).isEqualTo("CANDIDAT");
    }
}
