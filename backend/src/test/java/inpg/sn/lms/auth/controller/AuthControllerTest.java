package inpg.sn.lms.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

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
                "newPassword", "trop-court"));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        verifyNoInteractions(otpService, userService);
    }
}
