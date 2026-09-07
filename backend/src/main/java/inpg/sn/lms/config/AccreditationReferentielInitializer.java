package inpg.sn.lms.config;

import java.io.InputStream;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import inpg.sn.lms.accreditation.entity.CritereAccreditation;
import inpg.sn.lms.accreditation.repository.CritereAccreditationRepository;

/**
 * Amorçage du référentiel d'accréditation de la DRCQ (Direction de la Régulation
 * et du Contrôle Qualité de l'INPG), version révisée du 23 mars 2026 : 31
 * conditions codées (1.1 … 4.5) réparties en 4 sections.
 *
 * <p>La table {@code criteres_accreditation} existait depuis la V15 mais restait
 * vide : l'évaluateur ne disposait d'aucune grille de lecture. Le contenu est
 * tenu dans {@code data/referentiel-accreditation.json} plutôt qu'en dur ici —
 * même choix que {@link CatalogueDataInitializer} pour le catalogue.
 *
 * <p>Idempotent : un critère est inséré seulement si son {@code code} est absent
 * en base. Un critère déjà présent n'est jamais réécrit, pour préserver les
 * retouches faites par l'INPG via le CRUD de l'espace évaluateur.
 */
@Configuration
public class AccreditationReferentielInitializer {

    private static final String RESSOURCE = "data/referentiel-accreditation.json";

    @Bean
    @Order(4) // Après les rôles (1), le cursus de démo (2) et le catalogue (3).
    CommandLineRunner initReferentielAccreditation(CritereAccreditationRepository critereRepository,
            ObjectMapper objectMapper) {
        return args -> {
            ClassPathResource ressource = new ClassPathResource(RESSOURCE);
            if (!ressource.exists()) {
                System.out.println("ℹ️ Référentiel accréditation : ressource " + RESSOURCE + " absente, amorçage ignoré.");
                return;
            }

            int creees = 0;
            int ordre = 0;
            try (InputStream flux = ressource.getInputStream()) {
                JsonNode racine = objectMapper.readTree(flux);
                JsonNode criteres = racine.get("criteres");
                if (criteres == null || !criteres.isArray()) {
                    System.out.println("⚠️ Référentiel accréditation : clé « criteres » absente ou invalide.");
                    return;
                }
                for (JsonNode entree : criteres) {
                    ordre++;
                    String code = texte(entree, "code");
                    if (code == null) {
                        continue;
                    }
                    if (critereRepository.findByCode(code).isPresent()) {
                        continue;
                    }
                    critereRepository.save(new CritereAccreditation(
                            code,
                            texte(entree, "section"),
                            texte(entree, "libelle"),
                            texte(entree, "description"),
                            ordre));
                    creees++;
                }
            }

            if (creees > 0) {
                System.out.println("✅ Référentiel accréditation DRCQ : " + creees + " critère(s) amorcé(s) depuis "
                        + RESSOURCE);
            }
        };
    }

    private String texte(JsonNode noeud, String champ) {
        JsonNode valeur = noeud.get(champ);
        return valeur == null || valeur.isNull() ? null : valeur.asText();
    }
}
