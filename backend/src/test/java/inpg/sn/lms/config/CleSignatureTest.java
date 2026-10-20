package inpg.sn.lms.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Régression : {@code cp .env.example .env} donne {@code CERTIFICAT_SIGNATURE_SECRET=}
 * (définie, vide). Spring ne retombe sur {@code JWT_SECRET} que pour une variable
 * <em>absente</em> : la clé HMAC était donc vide, et le backend plantait après
 * « Started » (« Empty key ») dans le chargement des données de démonstration.
 */
class CleSignatureTest {

    private static final String JWT = "secret-jwt-de-test-suffisamment-long";

    @Test
    @DisplayName("La clé dédiée l'emporte quand elle est renseignée")
    void cleDedieeL_emporte() {
        assertThat(CleSignature.effective("cle-dediee", JWT)).isEqualTo("cle-dediee");
    }

    @Test
    @DisplayName("Une variable définie mais vide ou blanche vaut « non définie » : repli sur JWT_SECRET")
    void variableVideRetombeSurJwt() {
        assertThat(CleSignature.effective("", JWT)).isEqualTo(JWT);
        assertThat(CleSignature.effective("   ", JWT)).isEqualTo(JWT);
        assertThat(CleSignature.effective(null, JWT)).isEqualTo(JWT);
    }

    @Test
    @DisplayName("Sans aucune clé, le démarrage échoue en nommant les variables à renseigner")
    void sansAucuneCleLeDemarrageEchoueClairement() {
        assertThatThrownBy(() -> CleSignature.effective("", ""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CERTIFICAT_SIGNATURE_SECRET")
                .hasMessageContaining("JWT_SECRET");
        assertThatThrownBy(() -> CleSignature.effective(null, null))
                .isInstanceOf(IllegalStateException.class);
    }
}
