package inpg.sn.lms.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PasswordGeneratorServiceTest {

    private final PasswordGeneratorService service = new PasswordGeneratorService();

    @Test
    @DisplayName("Longueur de 12 caractères, mêlant majuscule, minuscule, chiffre et caractère spécial")
    void genere_un_mot_de_passe_mixte() {
        String motDePasse = service.generer();

        assertThat(motDePasse).hasSize(12);
        assertThat(motDePasse).matches(".*[a-z].*");
        assertThat(motDePasse).matches(".*[A-Z].*");
        assertThat(motDePasse).matches(".*[0-9].*");
        assertThat(motDePasse).matches(".*[!@#$%&*].*");
    }

    @Test
    @DisplayName("Deux appels ne renvoient jamais le même mot de passe")
    void ne_repete_pas_le_meme_mot_de_passe() {
        long distincts = IntStream.range(0, 20).mapToObj(i -> service.generer()).distinct().count();

        assertThat(distincts).isEqualTo(20);
    }
}
