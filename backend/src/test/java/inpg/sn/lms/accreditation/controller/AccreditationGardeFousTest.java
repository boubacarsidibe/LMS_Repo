package inpg.sn.lms.accreditation.controller;

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
 * Garde-fou structurel du paquet accréditation, même principe que {@code
 * EspaceEtudiantGardeFousTest} (CLAUDE.md §3) : {@code DemandeAccreditation.ecole}
 * est chargée paresseusement (open-in-view=false), donc tout endpoint qui la lit
 * — directement ou via un DTO — sans {@code @Transactional} ouvert renvoie une
 * {@code LazyInitializationException} (500) en production, invisible dans un
 * test unitaire qui instancie le contrôleur à la main (l'association est alors
 * un simple objet Java, jamais un proxy Hibernate).
 *
 * <p>Posé après coup, à l'endroit où le défaut est réellement survenu : {@code
 * AttestationAccreditationPublicController.verifier} lisait {@code
 * ecole.getNomEcole()} sans transaction — 57 tests Mockito verts n'avaient rien
 * vu, seule une vérification en conditions réelles (Playwright + backend réel)
 * l'a révélé. Posé globalement plutôt que corrigé au cas par cas : un endpoint
 * ajouté demain à l'un de ces contrôleurs sans {@code @Transactional} fera
 * échouer ce test.
 */
class AccreditationGardeFousTest {

    private static final List<Class<? extends Annotation>> ANNOTATIONS_DE_ROUTE = List.of(
            GetMapping.class, PostMapping.class, PutMapping.class,
            PatchMapping.class, DeleteMapping.class, RequestMapping.class);

    private static final List<Class<?>> CONTROLEURS = List.of(
            AccreditationPublicController.class,
            AttestationAccreditationPublicController.class,
            EcoleAccreditationController.class,
            EvaluateurAccreditationController.class);

    private static boolean estUneRoute(Method methode) {
        return ANNOTATIONS_DE_ROUTE.stream().anyMatch(methode::isAnnotationPresent);
    }

    @Test
    void chaque_endpoint_ouvre_sa_transaction() {
        for (Class<?> controleur : CONTROLEURS) {
            List<String> sansTransaction = Arrays.stream(controleur.getDeclaredMethods())
                    .filter(AccreditationGardeFousTest::estUneRoute)
                    .filter(methode -> !methode.isAnnotationPresent(Transactional.class))
                    .map(Method::getName)
                    .sorted()
                    .toList();

            assertTrue(sansTransaction.isEmpty(),
                    controleur.getSimpleName() + " : ces endpoints n'ouvrent aucune transaction, toute lecture "
                            + "d'une association paresseuse (ecole, pieces…) y renverra une erreur 500 en production : "
                            + sansTransaction);
        }
    }
}
