package inpg.sn.lms.recrutement.enums;

import java.util.Arrays;
import java.util.List;

/** Etapes explicites du parcours de recrutement d'une candidature. */
public enum EnumStatutCandidature {
    BROUILLON,
    SOUMIS,
    INCOMPLET,
    DOSSIER_VALIDE,
    DOSSIER_REJETE,
    PRESELECTIONNE,
    PAIEMENT_EN_ATTENTE,
    PAIEMENT_CONFIRME,
    CONVOQUE_TEST,
    TEST_REUSSI,
    TEST_ECHOUE,
    ABSENT_TEST,
    CONVOQUE_ENTRETIEN,
    ENTRETIEN_EFFECTUE,
    ABSENT_ENTRETIEN,
    ADMIS,
    LISTE_ATTENTE,
    NON_ADMIS,
    INSCRIPTION_FINALISEE,
    DESISTEMENT;

    /**
     * Analyse un filtre de statuts passé en paramètre d'URL : « SOUMIS,INCOMPLET » donne deux statuts, une valeur vide
     * ou absente donne une liste vide (= aucun filtre). Un onglet de la liste des candidatures regroupe plusieurs
     * statuts ; l'export doit pouvoir les recevoir tous, sans quoi il ne peut exporter que ce qu'un seul statut
     * désigne. Un code inconnu — par exemple l'ancien « ADMIS1 » — lève {@link IllegalArgumentException}.
     */
    public static List<EnumStatutCandidature> depuisListe(String filtre) {
        if (filtre == null || filtre.isBlank()) {
            return List.of();
        }
        return Arrays.stream(filtre.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> EnumStatutCandidature.valueOf(s.toUpperCase()))
                .distinct()
                .toList();
    }
}
