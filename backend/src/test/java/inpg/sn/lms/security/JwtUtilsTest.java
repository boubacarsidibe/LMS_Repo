package inpg.sn.lms.security;

import static org.assertj.core.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import inpg.sn.lms.auth.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

class JwtUtilsTest {
    private static final String KEY = "cle-de-test-uniquement-sans-valeur-en-production-01234567890123456789";
    private final JwtUtils jwt = new JwtUtils();
    private final User user = new User("alice", "hash", "alice@example.test");

    @BeforeEach void setup() {
        ReflectionTestUtils.setField(jwt, "secretString", KEY);
        ReflectionTestUtils.setField(jwt, "expirationTime", 600000L);
    }

    @Test void deux_connexions_produisent_des_jetons_distincts() {
        assertThat(jwt.generateToken(user)).isNotEqualTo(jwt.generateToken(user));
    }

    @Test void jeton_valide_puis_revoque() {
        String token = jwt.generateToken(user);
        assertThat(jwt.validateToken(token, user)).isTrue();
        user.incrementSessionVersion();
        assertThat(jwt.validateToken(token, user)).isFalse();
    }

    @Test void refuse_jeton_expire_ou_signature_invalide() {
        ReflectionTestUtils.setField(jwt, "expirationTime", -1000L);
        assertThat(jwt.validateToken(jwt.generateToken(user), user)).isFalse();
        ReflectionTestUtils.setField(jwt, "expirationTime", 600000L);
        String token = jwt.generateToken(user);
        ReflectionTestUtils.setField(jwt, "secretString", KEY + "different");
        assertThat(jwt.validateToken(token, user)).isFalse();
        assertThat(jwt.validateToken("illisible", user)).isFalse();
    }

    @Test void refuse_ancien_jeton_sans_version_et_version_fractionnaire() {
        for (Object version : new Object[] { "0", 0.5 }) {
            String token = Jwts.builder().setSubject("alice").claim("sessionVersion", version)
                    .setExpiration(new Date(System.currentTimeMillis() + 60000))
                    .signWith(Keys.hmacShaKeyFor(KEY.getBytes(StandardCharsets.UTF_8))).compact();
            assertThat(jwt.validateToken(token, user)).isFalse();
        }
        String legacy = Jwts.builder().setSubject("alice")
                .setExpiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(Keys.hmacShaKeyFor(KEY.getBytes(StandardCharsets.UTF_8))).compact();
        assertThat(jwt.validateToken(legacy, user)).isFalse();
    }
}
