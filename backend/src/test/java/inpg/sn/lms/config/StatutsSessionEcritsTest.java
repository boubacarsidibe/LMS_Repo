package inpg.sn.lms.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import inpg.sn.lms.recrutement.entity.SessionCandidature;

/**
 * Le statut d'une session est une chaîne libre côté Java, mais la base n'en accepte que cinq valeurs
 * ({@code sessions_candidature_status_check}). L'initialiseur de démonstration écrivait « OUVERT » et « CLOTURE » : sur une
 * base neuve, le serveur refusait de démarrer (audit du 2026-10-14), sans qu'aucun test ni la recette ne le voient,
 * puisque l'initialiseur ne s'exécute que sur une base vide. Ce test relit le code qui manipule des sessions.
 */
class StatutsSessionEcritsTest {

    private static final Pattern LITTERAL = Pattern.compile("setStatus\\(\\s*\"([A-Z_]+)\"\\s*\\)");

    @Test
    void tout_statut_de_session_ecrit_dans_le_code_est_accepte_par_la_base() throws IOException {
        List<String> refuses = new ArrayList<>();
        try (Stream<Path> fichiers = Files.walk(Path.of("src/main/java"))) {
            for (Path f : fichiers.filter(p -> p.toString().endsWith(".java")).toList()) {
                String source = Files.readString(f);
                if (!source.contains("SessionCandidature")) {
                    continue;
                }
                Matcher m = LITTERAL.matcher(source);
                while (m.find()) {
                    if (!SessionCandidature.STATUTS.contains(m.group(1))) {
                        refuses.add(f.getFileName() + " : " + m.group(1));
                    }
                }
            }
        }
        assertThat(refuses).as("valeurs refusées par sessions_candidature_status_check").isEmpty();
    }

    /** Les constantes suivent la contrainte de la base (migration V43 et suivantes). */
    @Test
    void les_constantes_sont_celles_de_la_contrainte() {
        assertThat(SessionCandidature.STATUTS)
                .containsExactlyInAnyOrder("BROUILLON", "OUVERTE", "SUSPENDUE", "CLOTUREE", "TERMINEE");
    }
}
