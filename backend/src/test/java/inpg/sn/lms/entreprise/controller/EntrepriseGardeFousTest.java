package inpg.sn.lms.entreprise.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Garde-fou structurel du paquet entreprise, même principe que {@code
 * AccreditationGardeFousTest} (posé après qu'une {@code LazyInitializationException}
 * a été trouvée en conditions réelles sur un endpoint public d'accréditation sans
 * {@code @Transactional} — voir CLAUDE.md §3). {@code DemandeEntreprise.entreprise}
 * et {@code FicheTechniqueModule.demande} sont chargées paresseusement : un
 * endpoint qui les lit sans transaction ouverte renvoie 500 en production,
 * invisible dans un test Mockito qui instancie le contrôleur à la main.
 */
class EntrepriseGardeFousTest {

    private static final List<Class<? extends Annotation>> ANNOTATIONS_DE_ROUTE = List.of(
            GetMapping.class, PostMapping.class, PutMapping.class,
            PatchMapping.class, DeleteMapping.class, RequestMapping.class);

    private static final List<Class<?>> CONTROLEURS = List.of(
            EntrepriseController.class,
            PedagogieEntrepriseController.class);

    private static boolean estUneRoute(Method methode) {
        return ANNOTATIONS_DE_ROUTE.stream().anyMatch(methode::isAnnotationPresent);
    }

    @Test
    void chaque_endpoint_ouvre_sa_transaction() {
        for (Class<?> controleur : CONTROLEURS) {
            List<String> sansTransaction = Arrays.stream(controleur.getDeclaredMethods())
                    .filter(EntrepriseGardeFousTest::estUneRoute)
                    .filter(methode -> !methode.isAnnotationPresent(Transactional.class))
                    .map(Method::getName)
                    .sorted()
                    .toList();

            assertTrue(sansTransaction.isEmpty(),
                    controleur.getSimpleName() + " : ces endpoints n'ouvrent aucune transaction, toute lecture "
                            + "d'une association paresseuse (entreprise, demande…) y renverra une erreur 500 en "
                            + "production : " + sansTransaction);
        }
    }
}
