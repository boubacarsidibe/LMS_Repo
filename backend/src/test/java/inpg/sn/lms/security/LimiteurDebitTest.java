package inpg.sn.lms.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class LimiteurDebitTest {

    /** Horloge réglable : le test avance le temps sans attendre. */
    private static final class Horloge extends Clock {
        Instant maintenant = Instant.parse("2026-10-14T10:00:00Z");
        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return maintenant; }
        void avancer(Duration d) { maintenant = maintenant.plus(d); }
    }

    private final Horloge horloge = new Horloge();
    private final LimiteurDebit limiteur = new LimiteurDebit(horloge);
    private static final Duration FENETRE = Duration.ofMinutes(10);

    @Test
    void autorise_jusqu_au_maximum_puis_refuse_en_disant_combien_attendre() {
        for (int i = 0; i < 3; i++) {
            assertThat(limiteur.essayer("r", "1.2.3.4", 3, FENETRE)).isEmpty();
        }
        assertThat(limiteur.essayer("r", "1.2.3.4", 3, FENETRE)).hasValue(601);   // 10 min + 1 s d'arrondi
    }

    @Test
    void l_attente_annoncee_diminue_avec_le_temps() {
        limiteur.essayer("r", "a", 1, FENETRE);
        horloge.avancer(Duration.ofMinutes(4));

        assertThat(limiteur.essayer("r", "a", 1, FENETRE)).hasValue(361);
    }

    @Test
    void la_fenetre_est_glissante_les_anciens_passages_expirent_un_par_un() {
        limiteur.essayer("r", "a", 2, FENETRE);                      // t = 0
        horloge.avancer(Duration.ofMinutes(6));
        limiteur.essayer("r", "a", 2, FENETRE);                      // t = 6
        assertThat(limiteur.essayer("r", "a", 2, FENETRE)).isPresent();

        horloge.avancer(Duration.ofMinutes(5));                      // t = 11 : le premier passage a expiré, pas le second
        assertThat(limiteur.essayer("r", "a", 2, FENETRE)).isEmpty();
        assertThat(limiteur.essayer("r", "a", 2, FENETRE)).isPresent();
    }

    @Test
    void chaque_adresse_et_chaque_regle_ont_leur_propre_compteur() {
        assertThat(limiteur.essayer("inscription", "a", 1, FENETRE)).isEmpty();
        assertThat(limiteur.essayer("inscription", "a", 1, FENETRE)).isPresent();

        assertThat(limiteur.essayer("inscription", "b", 1, FENETRE)).as("autre adresse").isEmpty();
        assertThat(limiteur.essayer("question", "a", 1, FENETRE)).as("autre règle").isEmpty();
    }

    @Test
    void un_refus_ne_prolonge_pas_le_blocage() {
        limiteur.essayer("r", "a", 1, FENETRE);
        for (int i = 0; i < 50; i++) {
            limiteur.essayer("r", "a", 1, FENETRE);                  // un visiteur insistant
        }
        horloge.avancer(FENETRE);

        assertThat(limiteur.essayer("r", "a", 1, FENETRE)).as("débloqué à l'heure dite malgré l'insistance").isEmpty();
    }

    @Test
    void la_memoire_reste_bornee_meme_face_a_des_milliers_d_adresses() {
        for (int i = 0; i < 60_000; i++) {
            limiteur.essayer("r", "10.0." + (i / 256) + "." + (i % 256), 1, FENETRE);
        }
        // 50 000 clés au plus sont retenues : la première adresse, la plus ancienne, a été oubliée.
        assertThat(limiteur.essayer("r", "10.0.0.0", 1, FENETRE)).isEmpty();
    }
}
