package inpg.sn.lms.auth.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PasswordPolicyTest {

    @Test
    @DisplayName("Accepte un mot de passe de douze caractères")
    void accepte_la_longueur_minimale() {
        assertThatCode(() -> PasswordPolicy.exigerMotDePasseRobuste("Abcd1234!xyz"))
                .doesNotThrowAnyException();
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
