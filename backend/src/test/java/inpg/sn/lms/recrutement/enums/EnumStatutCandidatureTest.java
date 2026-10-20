package inpg.sn.lms.recrutement.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Les onglets de la liste des candidatures regroupent plusieurs statuts, et l'export Excel ne recevait qu'un statut
 * unique : l'onglet « Test » envoyait l'ancien code « ADMIS1 », que le serveur ne connaît plus, et l'export répondait 400.
 */
class EnumStatutCandidatureTest {

    @Test
    @DisplayName("Un filtre vide ou absent n'exclut aucun statut")
    void filtreVide() {
        assertThat(EnumStatutCandidature.depuisListe(null)).isEmpty();
        assertThat(EnumStatutCandidature.depuisListe("")).isEmpty();
        assertThat(EnumStatutCandidature.depuisListe("  ")).isEmpty();
    }

    @Test
    @DisplayName("Plusieurs statuts séparés par des virgules sont tous retenus, sans doublon, espaces et casse tolérés")
    void plusieursStatuts() {
        assertThat(EnumStatutCandidature.depuisListe("SOUMIS, incomplet ,DOSSIER_VALIDE,SOUMIS"))
                .containsExactly(EnumStatutCandidature.SOUMIS, EnumStatutCandidature.INCOMPLET,
                        EnumStatutCandidature.DOSSIER_VALIDE);
    }

    @Test
    @DisplayName("Un statut seul fonctionne comme avant")
    void statutUnique() {
        assertThat(EnumStatutCandidature.depuisListe("ADMIS")).containsExactly(EnumStatutCandidature.ADMIS);
    }

    @Test
    @DisplayName("Un ancien code (ADMIS1, REJETE…) est refusé : il ne désigne plus aucun statut")
    void ancienCodeRefuse() {
        assertThatThrownBy(() -> EnumStatutCandidature.depuisListe("ADMIS1")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EnumStatutCandidature.depuisListe("SOUMIS,REJETE")).isInstanceOf(IllegalArgumentException.class);
    }
}
