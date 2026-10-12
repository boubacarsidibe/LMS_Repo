package inpg.sn.lms.recrutement.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import inpg.sn.lms.espace_etudiant.repository.FormationRepository;
import inpg.sn.lms.recrutement.service.NotificationService;
import inpg.sn.lms.recrutement.service.SessionCandidatureService;

/** Le formulaire de contact public écrit à l'adresse désignée par l'INPG, pas à celle des alertes d'admission. */
class PublicControllerContactTest {

    private final NotificationService notificationService = mock(NotificationService.class);
    private final PublicController controller = new PublicController(mock(SessionCandidatureService.class),
            notificationService, mock(FormationRepository.class), new ObjectMapper());

    @Test
    @DisplayName("sans réglage d'environnement, l'adresse de réception est training@inpg.sn")
    void adresse_de_reception_par_defaut() throws Exception {
        StandardEnvironment environnement = new StandardEnvironment();
        environnement.getPropertySources().addLast(new ResourcePropertySource("classpath:application.properties"));

        assertThat(environnement.getProperty("app.contact.email")).isEqualTo("training@inpg.sn");
    }

    @Test
    @DisplayName("le message du visiteur part vers l'adresse de contact, avec son e-mail en objet")
    void message_envoye_a_l_adresse_de_contact() {
        ReflectionTestUtils.setField(controller, "emailContact", "training@inpg.sn");
        when(notificationService.envoyerEmail(anyString(), anyString(), anyString())).thenReturn(true);

        var reponse = controller.poserQuestion(Map.of("email", "visiteur@exemple.sn", "message", "Bonjour"));

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(notificationService).envoyerEmail(org.mockito.ArgumentMatchers.eq("training@inpg.sn"),
                org.mockito.ArgumentMatchers.contains("visiteur@exemple.sn"), anyString());
    }
}
