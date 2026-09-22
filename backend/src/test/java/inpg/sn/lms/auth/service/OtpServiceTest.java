package inpg.sn.lms.auth.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

class OtpServiceTest {
    private final Instant now = Instant.parse("2026-09-16T12:00:00Z");
    private final OtpService service = new OtpService(mock(JavaMailSender.class), Clock.fixed(now, ZoneOffset.UTC));
    private void advance(int seconds) {
        ReflectionTestUtils.setField(service, "clock", Clock.fixed(now.plusSeconds(seconds), ZoneOffset.UTC));
    }

    @Test void code_consomme_une_seule_fois() {
        String code = service.generateOtp("alice@example.test");
        assertThat(service.validateOtp("alice@example.test", code)).isTrue();
        assertThat(service.validateOtp("alice@example.test", code)).isFalse();
        assertThatIllegalStateException().isThrownBy(() -> service.generateOtp("alice@example.test"));
    }

    @Test void cinq_echecs_ne_permettent_pas_de_contourner_la_limite_par_renvoi() {
        String code = service.generateOtp("alice@example.test");
        for (int i = 0; i < 5; i++) assertThat(service.validateOtp("alice@example.test", "000000")).isFalse();
        advance(61);
        assertThat(service.validateOtp("alice@example.test", code)).isFalse();
        assertThatIllegalStateException().isThrownBy(() -> service.generateOtp("alice@example.test"));
        advance(300);
        assertThat(service.generateOtp("alice@example.test")).hasSize(6);
    }

    @Test void expiration_exacte_a_cinq_minutes() {
        String code = service.generateOtp("alice@example.test");
        advance(300);
        assertThat(service.validateOtp("alice@example.test", code)).isFalse();
    }

    @Test void panne_smtp_invalide_le_code_mais_conserve_le_delai() {
        String code = service.generateOtp("alice@example.test");
        service.invalidateOtp("alice@example.test");
        assertThat(service.validateOtp("alice@example.test", code)).isFalse();
        assertThatIllegalStateException().isThrownBy(() -> service.generateOtp("alice@example.test"));
        advance(60);
        assertThat(service.generateOtp("alice@example.test")).hasSize(6);
    }

    @Test void verification_concurrente_ne_consomme_qu_une_fois() throws Exception {
        String code = service.generateOtp("alice@example.test");
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> service.validateOtp("alice@example.test", code));
            var second = executor.submit(() -> service.validateOtp("alice@example.test", code));
            assertThat(first.get() ^ second.get()).isTrue();
        } finally { executor.shutdownNow(); }
    }

    @Test void entree_absente_ne_provoque_pas_d_exception() {
        assertThat(service.validateOtp(null, null)).isFalse();
    }
}
