package inpg.sn.lms.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Configuration CORS unique de l'application.
 *
 * <p>Elle était auparavant déclarée à deux endroits — un bean {@code CorsFilter} ici
 * et un {@code addCorsMappings} dans {@link WebConfig} — avec des périmètres et des
 * méthodes autorisées différents. Deux sources de vérité pour une même règle, dont
 * une seule était réellement lue par Spring Security : de quoi faire perdre du temps
 * au premier problème de CORS.
 *
 * <p>Le bean exposé est un {@link CorsConfigurationSource}, c'est-à-dire précisément
 * ce que {@code http.cors(Customizer.withDefaults())} va chercher dans
 * {@link SecurityConfig}.
 */
@Configuration
public class CorsConfig {

    /**
     * Origines autorisées, séparées par des virgules. La valeur de repli couvre le
     * développement local ; en production, renseigner APP_CORS_ORIGINS avec le
     * domaine réel — le caractère générique est impossible ici, puisque les requêtes
     * portent des identifiants (allowCredentials).
     */
    @Value("${app.cors.origins:http://localhost:4200}")
    private String origines;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(origines.split("\\s*,\\s*")));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        // En-tête nécessaire au navigateur pour lire le nom du fichier lors des
        // téléchargements (exports Excel, reçus PDF, pièces justificatives).
        configuration.setExposedHeaders(List.of("Content-Disposition"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
