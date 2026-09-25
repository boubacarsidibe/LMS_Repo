package inpg.sn.lms.auth.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PasswordPolicyTest {

    @Test
    @DisplayName("Accepte un mot de passe de huit caractères")
    void accepte_la_longueur_minimale() {
        assertThatCode(() -> PasswordPolicy.exigerMotDePasseRobuste("Abcd123!"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Aucun plafond de longueur : un mot de passe très long est accepté")
    void pas_de_longueur_maximale() {
        assertThatCode(() -> PasswordPolicy.exigerMotDePasseRobuste("é".repeat(200))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Refuse un mot de passe fait uniquement d'espaces")
    void refuse_les_espaces_seuls() {
        assertThatIllegalArgumentException().isThrownBy(() -> PasswordPolicy.exigerMotDePasseRobuste(" ".repeat(12)));
    }

    @Test
    @DisplayName("Refuse un mot de passe court ou absent")
    void refuse_un_mot_de_passe_court_ou_absent() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> PasswordPolicy.exigerMotDePasseRobuste("Abcd123"));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> PasswordPolicy.exigerMotDePasseRobuste(null));
    }
}
