package inpg.sn.lms.auth.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PasswordPolicyTest {

    /**
     * Construit sur {@link PasswordPolicy#LONGUEUR_MINIMALE} plutôt que sur un
     * nombre de caractères recopié en dur : la longueur minimale a déjà changé
     * une fois (douze caractères jugés trop contraignants pour la création de
     * compte, ramenés à huit) et ce test n'a pas à être retouché à chaque
     * nouvelle révision de la règle.
     */
    private static String motDePasse(int longueur) {
        String motif = "Abcd1234!xyz";
        StringBuilder resultat = new StringBuilder();
        while (resultat.length() < longueur) {
            resultat.append(motif);
        }
        return resultat.substring(0, longueur);
    }

    @Test
    @DisplayName("Accepte un mot de passe à la longueur minimale exacte")
    void accepte_la_longueur_minimale() {
        assertThatCode(() -> PasswordPolicy.exigerMotDePasseRobuste(motDePasse(PasswordPolicy.LONGUEUR_MINIMALE)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Refuse un mot de passe court ou absent")
    void refuse_un_mot_de_passe_court_ou_absent() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> PasswordPolicy.exigerMotDePasseRobuste(motDePasse(PasswordPolicy.LONGUEUR_MINIMALE - 1)));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> PasswordPolicy.exigerMotDePasseRobuste(null));
    }
}
