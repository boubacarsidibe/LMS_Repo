package inpg.sn.lms.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.auth.dto.AuthRequest;
import inpg.sn.lms.auth.service.LoginAttemptService;
import org.springframework.mail.MailSendException;

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

    @Mock private LoginAttemptService loginAttemptService;
    private AuthController controller;

    @BeforeEach
    void initialiserControleur() {
        controller = new AuthController(authenticationManager, jwtUtils, otpService, userService,
                passwordEncoder, formateurService, loginAttemptService);
    }

    @Test void reponses_otp_identiques_pour_compte_inconnu_et_panne_smtp() {
        when(otpService.generateOtp("alice@example.test")).thenReturn("123456");
        var absent = controller.sendOtp(Map.of("email", " ALICE@example.test "));
        when(userService.findByEmail("alice@example.test")).thenReturn(new User());
        doThrow(new MailSendException("smtp")).when(otpService).sendOtpEmail("alice@example.test", "123456");
        var panne = controller.sendOtp(Map.of("email", "alice@example.test"));
        assertThat(panne.getStatusCode()).isEqualTo(absent.getStatusCode());
        assertThat(panne.getBody()).isEqualTo(absent.getBody());
        verify(otpService).invalidateOtp("alice@example.test");
    }

    @Test void aucun_jeton_si_mot_de_passe_incorrect() {
        when(loginAttemptService.autoriser("alice@example.test")).thenReturn(true);
        when(userService.findByEmail("alice@example.test")).thenReturn(new User("alice", "hash", "alice@example.test"));
        when(authenticationManager.authenticate(any())).thenThrow(
                new org.springframework.security.authentication.BadCredentialsException("incorrect"));
        var response = controller.login(new AuthRequest("alice@example.test", "incorrect"));
        assertThat(response.getStatusCode().value()).isEqualTo(401);
        verifyNoInteractions(jwtUtils);
    }

    @Test void connexion_limitee_avant_verification_des_identifiants() {
        AuthRequest request = new AuthRequest();
        request.setEmail("alice@example.test");
        request.setPassword("mot-de-passe-long");
        var response = controller.login(request);
        assertThat(response.getStatusCode().value()).isEqualTo(429);
        verifyNoInteractions(userService, authenticationManager, jwtUtils);
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
