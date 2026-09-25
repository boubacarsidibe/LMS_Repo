package inpg.sn.lms.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class PasswordConfigTest {

    private final PasswordEncoder encodeur = new PasswordConfig().passwordEncoder();

    @Test
    @DisplayName("Un mot de passe court se hache et se vérifie")
    void mot_de_passe_court() {
        String hache = encodeur.encode("Abcd123!");
        assertThat(encodeur.matches("Abcd123!", hache)).isTrue();
        assertThat(encodeur.matches("Abcd123?", hache)).isFalse();
    }

    @Test
    @DisplayName("Un mot de passe de plus de 72 octets ne lève pas d'exception et reste distinguant")
    void mot_de_passe_tres_long() {
        String long1 = "a".repeat(100);
        String long2 = "a".repeat(99) + "b";
        String hache = encodeur.encode(long1);
        assertThat(encodeur.matches(long1, hache)).isTrue();
        assertThat(encodeur.matches(long2, hache)).isFalse();
    }

    @Test
    @DisplayName("Les hachages BCrypt déjà en base (mots de passe ≤ 72 octets) restent valides")
    void compatible_avec_les_hachages_existants() {
        String existant = new BCryptPasswordEncoder().encode("Test@2026");
        assertThat(encodeur.matches("Test@2026", existant)).isTrue();
    }
}
