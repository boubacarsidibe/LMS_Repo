package inpg.sn.lms.espace_etudiant.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import inpg.sn.lms.espace_etudiant.dto.RapportFormateursDto;

/**
 * {@link RapportFormateursPdfService} : mise en page du rapport formateurs,
 * sans recalcul, robuste à un effectif vide et à un formateur sans cours.
 */
class RapportFormateursPdfServiceTest {

    private final RapportFormateursPdfService service = new RapportFormateursPdfService();

    @Test
    @DisplayName("PDF valide avec un formateur affecté et un formateur sans cours")
    void rapport_valide_avec_et_sans_cours() {
        RapportFormateursDto.FormateurRapport avecCours = new RapportFormateursDto.FormateurRapport(
                "Diop", "Awa 🎓", "awa@example.com", "+221770000001", "adiop",
                LocalDateTime.now(), List.of("Thermodynamique", "Mécanique des fluides"));
        RapportFormateursDto.FormateurRapport sansCours = new RapportFormateursDto.FormateurRapport(
                "Sarr", "Modou", "modou@example.com", "+221770000002", "msarr",
                LocalDateTime.now(), List.of());

        RapportFormateursDto rapport = new RapportFormateursDto(2, 1, List.of(avecCours, sansCours));

        byte[] pdf = service.genererRapport(rapport);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("Aucun formateur : PDF valide malgré un effectif vide")
    void rapport_valide_sans_aucun_formateur() {
        RapportFormateursDto rapport = new RapportFormateursDto(0, 0, List.of());

        assertThat(service.genererRapport(rapport)).isNotEmpty();
    }
}
