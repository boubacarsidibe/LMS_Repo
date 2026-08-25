package inpg.sn.lms.espace_etudiant.controller;

import static org.junit.jupiter.api.Assertions.assertNotNull;
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
 * Garde-fous structurels de l'espace étudiant.
 *
 * <p>Ces vérifications portent sur les annotations, pas sur le comportement. C'est
 * délibéré : les deux défauts qu'elles figent ne sont pas reproductibles avec des
 * mocks.
 *
 * <ul>
 *   <li>Un {@code @Transactional} manquant ne se voit qu'avec une vraie session
 *       Hibernate. Instancié à la main dans un test unitaire, le contrôleur ne passe
 *       par aucun proxy Spring : l'association paresseuse est un simple objet Java et
 *       la lecture réussit. En production, avec {@code open-in-view=false}, la même
 *       ligne lève une {@code LazyInitializationException} et l'écran renvoie 500.</li>
 *   <li>Un {@code @PreAuthorize} manquant est invisible tant que les règles d'URL de
 *       {@code SecurityConfig} couvrent le chemin. Elles le couvrent aujourd'hui — mais
 *       un renommage d'URL suffirait à ouvrir l'endpoint sans qu'aucun test n'échoue.</li>
 * </ul>
 *
 * <p>Ce contrôleur ne porte pas d'annotation de classe : chaque méthode doit donc
 * déclarer la sienne. C'est cette absence qui avait laissé passer les deux cas.
 */
class EspaceEtudiantGardeFousTest {

    private static final List<Class<? extends Annotation>> ANNOTATIONS_DE_ROUTE = List.of(
            GetMapping.class, PostMapping.class, PutMapping.class,
            PatchMapping.class, DeleteMapping.class, RequestMapping.class);

    private static boolean estUneRoute(Method methode) {
        return ANNOTATIONS_DE_ROUTE.stream().anyMatch(methode::isAnnotationPresent);
    }

    /**
     * Chaque endpoint déclare son contrôle de rôle.
     *
     * <p>Se protège de toute la famille de défauts, pas seulement des deux corrigés :
     * un endpoint ajouté demain sans {@code @PreAuthorize} fera échouer ce test.
     */
    @Test
    void chaque_endpoint_declare_son_controle_de_role() {
        List<String> sansControle = Arrays.stream(EspaceEtudiantController.class.getDeclaredMethods())
                .filter(EspaceEtudiantGardeFousTest::estUneRoute)
                .filter(methode -> !methode.isAnnotationPresent(PreAuthorize.class))
                .map(Method::getName)
                .sorted()
                .toList();

        assertTrue(sansControle.isEmpty(),
                "Ces endpoints ne déclarent aucun @PreAuthorize et ne sont donc protégés "
                        + "que par les règles d'URL de SecurityConfig : " + sansControle);
    }

    /**
     * La fiche de suivi d'un étudiant lit la promotion, chargée paresseusement, au
     * moment de construire sa réponse. Sans transaction ouverte, l'écran
     * « Suivi des étudiants » renvoyait systématiquement une erreur 500.
     */
    @Test
    void la_fiche_etudiant_lit_la_promotion_dans_une_transaction() throws NoSuchMethodException {
        Method fiche = EspaceEtudiantController.class.getDeclaredMethod(
                "getStudent", Long.class, org.springframework.security.core.Authentication.class);

        assertNotNull(fiche.getAnnotation(Transactional.class),
                "getStudent doit rester @Transactional : toEtudiantSuiviDto lit la promotion "
                        + "en association paresseuse, et open-in-view est désactivé.");
    }

    /**
     * Chaque endpoint ouvre sa transaction.
     *
     * <p>Ce contrôleur construit ses réponses en traversant des associations chargées
     * paresseusement — la promotion d'un étudiant, les formations d'un cours. Avec
     * {@code open-in-view=false}, la session Hibernate se ferme à la sortie du service :
     * toute lecture faite ensuite lève une {@code LazyInitializationException} et
     * l'écran renvoie 500.
     *
     * <p>Ce défaut est survenu <strong>trois fois</strong> sur ce fichier, à chaque fois
     * sur un endpoint différent, et à chaque fois invisible en test unitaire. La règle
     * est donc posée globalement plutôt que constat par constat : un endpoint ajouté
     * sans transaction fera échouer ce test.
     */
    @Test
    void chaque_endpoint_ouvre_sa_transaction() {
        List<String> sansTransaction = Arrays.stream(EspaceEtudiantController.class.getDeclaredMethods())
                .filter(EspaceEtudiantGardeFousTest::estUneRoute)
                .filter(methode -> !methode.isAnnotationPresent(Transactional.class))
                .map(Method::getName)
                .sorted()
                .toList();

        assertTrue(sansTransaction.isEmpty(),
                "Ces endpoints n'ouvrent aucune transaction : toute lecture d'une association "
                        + "paresseuse y renverra une erreur 500 en production. " + sansTransaction);
    }
}
