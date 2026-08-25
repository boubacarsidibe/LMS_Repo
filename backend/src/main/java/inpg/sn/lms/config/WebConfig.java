package inpg.sn.lms.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Sous-dossier des uploads réellement exposé sans authentification. Tout ce qui
     * doit rester privé est écrit ailleurs, en dehors de ce dossier.
     */
    public static final String DOSSIER_PUBLIC = "public";

    @Value("${app.upload.dir:uploads}")
    private String uploadDirectory;

    // La configuration CORS vit désormais uniquement dans CorsConfig : une règle,
    // un endroit. Ne pas rajouter d'addCorsMappings ici.

    /**
     * Seul le sous-dossier {@code public} est exposé en statique.
     *
     * <p>Auparavant c'était la racine des uploads, donc les pièces justificatives des
     * candidats (CV, diplômes, pièces d'identité), les supports de cours et les pièces
     * jointes du forum étaient tous téléchargeables sans authentification pour qui
     * connaissait l'URL. Ils restent servis par leurs endpoints authentifiés respectifs.
     *
     * <p>L'URL vue du navigateur ne change pas : {@code /uploads/image.png} pointe
     * désormais vers {@code <app.upload.dir>/public/image.png}.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = java.nio.file.Path.of(uploadDirectory)
                .resolve(DOSSIER_PUBLIC)
                .toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}