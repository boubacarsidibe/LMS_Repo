package inpg.sn.lms.auth.service;

import static org.assertj.core.api.Assertions.*;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class LoginAttemptServiceTest {
    @Test void limite_normalisee_et_expiration() {
        Instant maintenant = Instant.parse("2026-09-16T12:00:00Z");
        var service = new LoginAttemptService(Clock.fixed(maintenant, ZoneOffset.UTC));
        for (int i = 0; i < 10; i++) assertThat(service.autoriser(" ALICE@example.test ")).isTrue();
        assertThat(service.autoriser("alice@example.test")).isFalse();
        assertThat(service.autoriser("bob@example.test")).isTrue();
        ReflectionTestUtils.setField(service, "clock", Clock.fixed(maintenant.plusSeconds(900), ZoneOffset.UTC));
        assertThat(service.autoriser("alice@example.test")).isTrue();
    }
}
