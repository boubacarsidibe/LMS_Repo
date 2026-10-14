package inpg.sn.lms.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Inscription, question publique et vérification de certificat étaient ouvertes sans aucune limite (audit du 2026-10-14) :
 * comptes créés en masse, e-mails à l'INPG à volonté, références de certificats balayées.
 */
class LimiteDebitFilterTest {

    private final LimiteDebitFilter filtre = new LimiteDebitFilter(true, 3, 2, 4);

    private MockHttpServletResponse appeler(String methode, String chemin, String xForwardedFor) throws Exception {
        MockHttpServletRequest requete = new MockHttpServletRequest(methode, chemin);
        requete.setRemoteAddr("172.18.0.5");
        if (xForwardedFor != null) {
            requete.addHeader("X-Forwarded-For", xForwardedFor);
        }
        MockHttpServletResponse reponse = new MockHttpServletResponse();
        filtre.doFilter(requete, reponse, new MockFilterChain());
        return reponse;
    }

    @Test
    void l_inscription_est_limitee_par_adresse() throws Exception {
        for (int i = 0; i < 3; i++) {
            assertThat(appeler("POST", "/api/auth/register", "203.0.113.7").getStatus()).isEqualTo(200);
        }
        MockHttpServletResponse refus = appeler("POST", "/api/auth/register", "203.0.113.7");

        assertThat(refus.getStatus()).isEqualTo(429);
        assertThat(refus.getHeader("Retry-After")).isNotNull();
        assertThat(refus.getContentAsString(java.nio.charset.StandardCharsets.UTF_8)).contains("Trop de demandes").contains("minutes");
        assertThat(appeler("POST", "/api/auth/register", "203.0.113.8").getStatus()).as("autre visiteur").isEqualTo(200);
    }

    @Test
    void la_question_publique_et_la_verification_de_certificat_sont_limitees() throws Exception {
        appeler("POST", "/api/recrutement/public/question", "203.0.113.7");
        appeler("POST", "/api/recrutement/public/question", "203.0.113.7");
        assertThat(appeler("POST", "/api/recrutement/public/question", "203.0.113.7").getStatus()).isEqualTo(429);

        for (int i = 0; i < 4; i++) {
            appeler("GET", "/api/certificats/public/verifier/INPG-CERT-2026-AAAA000" + i, "203.0.113.7");
        }
        assertThat(appeler("GET", "/api/certificats/public/verifier/INPG-CERT-2026-BBBB0000", "203.0.113.7").getStatus()).isEqualTo(429);
    }

    @Test
    void les_autres_routes_ne_sont_jamais_limitees() throws Exception {
        for (int i = 0; i < 30; i++) {
            assertThat(appeler("GET", "/api/recrutement/public/catalogue", "203.0.113.7").getStatus()).isEqualTo(200);
            assertThat(appeler("GET", "/api/auth/register", "203.0.113.7").getStatus()).as("autre méthode").isEqualTo(200);
            assertThat(appeler("POST", "/api/auth/login", "203.0.113.7").getStatus()).isEqualTo(200);
        }
    }

    /** Les adresses qui précèdent sont écrites par le client : seule la dernière, ajoutée par le proxy, fait foi. */
    @Test
    void une_adresse_falsifiee_en_tete_de_l_en_tete_ne_contourne_pas_la_limite() throws Exception {
        for (int i = 0; i < 3; i++) {
            appeler("POST", "/api/auth/register", "10.0.0." + i + ", 203.0.113.7");
        }

        assertThat(appeler("POST", "/api/auth/register", "10.9.9.9, 203.0.113.7").getStatus()).isEqualTo(429);
    }

    @Test
    void sans_en_tete_c_est_l_adresse_de_la_connexion_qui_compte() throws Exception {
        for (int i = 0; i < 3; i++) {
            appeler("POST", "/api/auth/register", null);
        }

        assertThat(appeler("POST", "/api/auth/register", null).getStatus()).isEqualTo(429);
        assertThat(appeler("POST", "/api/auth/register", "203.0.113.7").getStatus()).isEqualTo(200);
    }

    @Test
    void la_limitation_peut_etre_coupee_par_configuration() throws Exception {
        LimiteDebitFilter inactif = new LimiteDebitFilter(false, 1, 1, 1);
        for (int i = 0; i < 5; i++) {
            MockHttpServletResponse r = new MockHttpServletResponse();
            inactif.doFilter(new MockHttpServletRequest("POST", "/api/auth/register"), r, new MockFilterChain());
            assertThat(r.getStatus()).isEqualTo(200);
        }
    }
}
