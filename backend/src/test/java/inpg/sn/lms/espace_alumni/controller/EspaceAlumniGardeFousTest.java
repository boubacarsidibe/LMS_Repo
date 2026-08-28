package inpg.sn.lms.espace_alumni.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Garde-fou structurel pour les trois contrôleurs de l'espace Alumni
 * ({@link AlumniController}, {@link AlumniForumController},
 * {@link AlumniDocumentController}).
 *
 * <p>Comble l'écart signalé au point 51 de {@code LOG_BLOQUANT.md} :
 * {@code EspaceEtudiantGardeFousTest} et {@code MessagerieControllerTest} figent
 * chacun une seule classe, aucun ne protège le paquet {@code espace_alumni}. Les
 * trois classes ici portent leur {@code @PreAuthorize} au niveau classe (jamais
 * méthode par méthode), la vérification porte donc sur la classe entière plutôt
 * que sur les méthodes, à la différence de {@code EspaceEtudiantGardeFousTest}
 * (qui n'a pas d'annotation de classe et doit vérifier chaque méthode).
 */
class EspaceAlumniGardeFousTest {

    private static final List<Class<?>> CONTROLEURS = List.of(
            AlumniController.class, AlumniForumController.class, AlumniDocumentController.class);

    private static final List<Class<? extends Annotation>> ANNOTATIONS_DE_ROUTE = List.of(
            GetMapping.class, PostMapping.class, PutMapping.class,
            PatchMapping.class, DeleteMapping.class, RequestMapping.class);

    private static boolean estUneRoute(Method methode) {
        return ANNOTATIONS_DE_ROUTE.stream().anyMatch(methode::isAnnotationPresent);
    }

    @Test
    void chaque_controleur_restreint_lacces_au_niveau_classe() {
        List<String> sansControle = CONTROLEURS.stream()
                .filter(controleur -> !controleur.isAnnotationPresent(PreAuthorize.class))
                .map(Class::getSimpleName)
                .toList();

        assertTrue(sansControle.isEmpty(),
                "Ces contrôleurs de l'espace alumni ne portent aucun @PreAuthorize de classe : "
                        + sansControle);
    }

    /**
     * Même défaut que celui corrigé trois fois sur {@code EspaceEtudiantController}
     * (open-in-view désactivé, association paresseuse lue hors session) : un
     * endpoint ajouté demain sans {@code @Transactional} dans l'un de ces trois
     * contrôleurs fera échouer ce test plutôt que de renvoyer un 500 en production.
     */
    @Test
    void chaque_endpoint_des_trois_controleurs_ouvre_sa_transaction() {
        List<String> sansTransaction = CONTROLEURS.stream()
                .flatMap(controleur -> Arrays.stream(controleur.getDeclaredMethods())
                        .filter(EspaceAlumniGardeFousTest::estUneRoute)
                        .filter(methode -> !methode.isAnnotationPresent(Transactional.class))
                        .map(methode -> controleur.getSimpleName() + "#" + methode.getName()))
                .sorted()
                .toList();

        assertTrue(sansTransaction.isEmpty(),
                "Ces endpoints n'ouvrent aucune transaction : toute lecture d'une association "
                        + "paresseuse y renverra une erreur 500 en production. " + sansTransaction);
    }
}
