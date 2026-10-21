package inpg.sn.lms.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import inpg.sn.lms.espace_etudiant.dto.ForumSujetCreateRequest;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring Boot 4 lit les requêtes JSON avec Jackson 3, dont l'option {@code FAIL_ON_NULL_FOR_PRIMITIVES} est ACTIVE par
 * défaut : un champ primitif absent du corps (ici {@code boolean pourMaPromotion}) fait échouer toute la requête, avec
 * un simple « Failed to read request » (HTTP 400). Le formulaire des alumni n'envoie pas ce champ — qui n'a de sens que
 * pour un étudiant — et ne pouvait donc ouvrir AUCUN sujet de forum. Jackson 2, sur lequel le code a été écrit, comptait
 * un champ absent pour {@code false}.
 *
 * <p>Le test charge la configuration réelle ({@code application.properties}) dans la vraie auto-configuration Jackson : lire
 * seulement un mapper construit à la main ne verrait pas la propriété.
 */
class JacksonChampsAbsentsTest {

    private final ApplicationContextRunner contexte = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class));

    @Test
    void un_champ_primitif_absent_vaut_sa_valeur_par_defaut() {
        contexte.run(ctx -> {
            JsonMapper mapper = ctx.getBean(JsonMapper.class);

            // Corps exact envoyé par le formulaire du forum des alumni : pas de « pourMaPromotion ».
            ForumSujetCreateRequest requete = mapper.readValue(
                    "{\"titre\":\"Un mentor\",\"coursId\":null,\"premierMessage\":\"Bonjour\"}", ForumSujetCreateRequest.class);

            assertThat(requete.titre()).isEqualTo("Un mentor");
            assertThat(requete.pourMaPromotion()).isFalse();
        });
    }
}
