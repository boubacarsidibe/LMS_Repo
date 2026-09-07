package inpg.sn.lms.config;

import java.io.InputStream;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import inpg.sn.lms.espace_etudiant.entity.Formation;
import inpg.sn.lms.espace_etudiant.repository.FormationRepository;

/**
 * Amorçage du catalogue public de formations.
 *
 * <p>Ces 16 formations vivaient jusqu'ici dans deux tableaux TypeScript, au cœur
 * des composants Angular : la vitrine de l'institut affichait des tarifs et des
 * dates de rentrée que personne ne pouvait corriger sans redéployer le front.
 * Elles sont désormais des entités {@link Formation}, servies par l'API publique
 * et modifiables par le service pédagogique.
 *
 * <p>Le contenu éditorial de chaque fiche (modules, débouchés, encadré latéral)
 * est conservé tel quel en JSON dans {@code ficheDetaillee} : le gabarit Angular
 * qui l'affichait n'a pas eu à changer.
 */
@Configuration
public class CatalogueDataInitializer {

    private static final String RESSOURCE = "data/catalogue-formations.json";

    @Bean
    @Order(3) // Après les rôles (1) et le cursus de démonstration (2).
    CommandLineRunner initCatalogue(FormationRepository formationRepository, ObjectMapper objectMapper) {
        return args -> {
            ClassPathResource ressource = new ClassPathResource(RESSOURCE);
            if (!ressource.exists()) {
                System.out.println("ℹ️ Catalogue : ressource " + RESSOURCE + " absente, amorçage ignoré.");
                return;
            }

            List<Formation> existantes = formationRepository.findAll();
            int creees = 0;

            try (InputStream flux = ressource.getInputStream()) {
                JsonNode entrees = objectMapper.readTree(flux);
                for (JsonNode entree : entrees) {
                    String code = texte(entree, "code");
                    if (code == null) {
                        continue;
                    }
                    // Le code fait office de clé d'idempotence : relancer
                    // l'application ne duplique pas le catalogue et n'écrase pas
                    // les retouches faites depuis le back-office.
                    Formation existante = existantes.stream()
                            .filter(f -> code.equals(f.getCode()))
                            .findFirst()
                            .orElse(null);
                    if (existante != null) {
                        boolean modifie = false;
                        if (!existante.isPubliee()) {
                            existante.setPubliee(true);
                            modifie = true;
                        }
                        if (existante.getFicheDetaillee() == null) {
                            JsonNode fiche = entree.get("ficheDetaillee");
                            if (fiche != null && !fiche.isNull()) {
                                existante.setFicheDetaillee(objectMapper.writeValueAsString(fiche));
                                modifie = true;
                            }
                        }
                        // Caractéristiques du §3 du Référentiel DRCQ : on ne complète que
                        // ce qui n'a pas encore de valeur, sans écraser une retouche back-office.
                        if (existante.getHeuresSimulationLabo() == null && entree.hasNonNull("heuresSimulationLabo")) {
                            existante.setHeuresSimulationLabo(entree.get("heuresSimulationLabo").asInt());
                            modifie = true;
                        }
                        if (!existante.isAnglaisProfessionnel() && entree.path("anglaisProfessionnel").asBoolean(false)) {
                            existante.setAnglaisProfessionnel(true);
                            modifie = true;
                        }
                        if (!existante.isSoftSkills() && entree.path("softSkills").asBoolean(false)) {
                            existante.setSoftSkills(true);
                            modifie = true;
                        }
                        if (existante.getCreditsTotaux() == null && entree.hasNonNull("creditsTotaux")) {
                            existante.setCreditsTotaux(entree.get("creditsTotaux").asInt());
                            modifie = true;
                        }
                        if (existante.getVolumeHoraireTotal() == null && entree.hasNonNull("volumeHoraireTotal")) {
                            existante.setVolumeHoraireTotal(entree.get("volumeHoraireTotal").asInt());
                            modifie = true;
                        }
                        if (modifie) {
                            formationRepository.save(existante);
                        }
                        continue;
                    }

                    Formation formation = new Formation();
                    formation.setCode(code);
                    formation.setIntitule(texte(entree, "intitule"));
                    formation.setDescription(texte(entree, "description"));
                    formation.setNiveau(texte(entree, "niveau"));
                    formation.setDomaine(texte(entree, "domaine"));
                    formation.setDuree(texte(entree, "duree"));
                    formation.setImageUrl(texte(entree, "imageUrl"));
                    formation.setPubliee(entree.path("publiee").asBoolean(true));
                    if (entree.hasNonNull("heuresSimulationLabo")) {
                        formation.setHeuresSimulationLabo(entree.get("heuresSimulationLabo").asInt());
                    }
                    formation.setAnglaisProfessionnel(entree.path("anglaisProfessionnel").asBoolean(false));
                    formation.setSoftSkills(entree.path("softSkills").asBoolean(false));
                    if (entree.hasNonNull("creditsTotaux")) {
                        formation.setCreditsTotaux(entree.get("creditsTotaux").asInt());
                    }
                    if (entree.hasNonNull("volumeHoraireTotal")) {
                        formation.setVolumeHoraireTotal(entree.get("volumeHoraireTotal").asInt());
                    }

                    JsonNode fiche = entree.get("ficheDetaillee");
                    if (fiche != null && !fiche.isNull()) {
                        formation.setFicheDetaillee(objectMapper.writeValueAsString(fiche));
                    }

                    formationRepository.save(formation);
                    creees++;
                }
            }

            if (creees > 0) {
                System.out.println("✅ Catalogue : " + creees + " formation(s) publiée(s) depuis " + RESSOURCE);
            }
        };
    }

    private String texte(JsonNode noeud, String champ) {
        JsonNode valeur = noeud.get(champ);
        return valeur == null || valeur.isNull() ? null : valeur.asText();
    }
}
