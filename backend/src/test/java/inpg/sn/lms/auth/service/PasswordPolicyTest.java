package inpg.sn.lms.auth.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PasswordPolicyTest {

    @Test
    @DisplayName("Accepte un mot de passe de douze caractères")
    void accepte_la_longueur_minimale() {
        assertThatCode(() -> PasswordPolicy.exigerMotDePasseRobuste("Abcd1234!xyza"))
                .doesNotThrowAnyException();
    }

    @Test
    void limite_bcrypt_en_octets_et_non_en_caracteres() {
        assertThatCode(() -> PasswordPolicy.exigerMotDePasseRobuste("é".repeat(36))).doesNotThrowAnyException();
        assertThatIllegalArgumentException().isThrownBy(() -> PasswordPolicy.exigerMotDePasseRobuste("é".repeat(37)));
        assertThatIllegalArgumentException().isThrownBy(() -> PasswordPolicy.exigerMotDePasseRobuste(" ".repeat(12)));
    }

    @Test
    @DisplayName("Refuse un mot de passe court ou absent")
    void refuse_un_mot_de_passe_court_ou_absent() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> PasswordPolicy.exigerMotDePasseRobuste("Abcd1234!xy"));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> PasswordPolicy.exigerMotDePasseRobuste(null));
    }
}
