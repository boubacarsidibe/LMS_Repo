package inpg.sn.lms.recrutement.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import inpg.sn.lms.recrutement.dto.RapportStatistiqueDto;

/**
 * {@link RapportStatistiquePdfService} : mise en page du rapport statistique
 * recrutement, sans recalcul, robuste à un rapport entièrement vide.
 */
class RapportStatistiquePdfServiceTest {

    private final RapportStatistiquePdfService service = new RapportStatistiquePdfService();

    @Test
    @DisplayName("PDF valide avec des données démographiques complètes")
    void rapport_valide_avec_donnees_completes() {
        RapportStatistiqueDto rapport = new RapportStatistiqueDto(
                10, 3, 4, 2,
                List.of(
                        new RapportStatistiqueDto.Segment("Soumis", 3, 30),
                        new RapportStatistiqueDto.Segment("En présélection", 2, 20),
                        new RapportStatistiqueDto.Segment("Admis au test", 2, 20),
                        new RapportStatistiqueDto.Segment("Admis à l'entretien", 1, 10),
                        new RapportStatistiqueDto.Segment("Admis final", 0, 0),
                        new RapportStatistiqueDto.Segment("Rejeté", 2, 20)),
                50, 4,
                List.of(new RapportStatistiqueDto.Segment("Homme", 6, 60),
                        new RapportStatistiqueDto.Segment("Femme", 4, 40)),
                List.of(new RapportStatistiqueDto.Segment("20-25 ans", 5, 50)),
                List.of(new RapportStatistiqueDto.Segment("Ingénieur 🎓", 3, 30)),
                2,
                List.of(new RapportStatistiqueDto.Segment("Sénégalaise", 8, 80)));

        byte[] pdf = service.genererRapport(rapport);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("Aucune candidature : PDF valide malgré des segments tous vides")
    void rapport_valide_sans_aucune_candidature() {
        List<RapportStatistiqueDto.Segment> statutsVides = List.of(
                new RapportStatistiqueDto.Segment("Soumis", 0, 0),
                new RapportStatistiqueDto.Segment("En présélection", 0, 0),
                new RapportStatistiqueDto.Segment("Admis au test", 0, 0),
                new RapportStatistiqueDto.Segment("Admis à l'entretien", 0, 0),
                new RapportStatistiqueDto.Segment("Admis final", 0, 0),
                new RapportStatistiqueDto.Segment("Rejeté", 0, 0));

        RapportStatistiqueDto rapport = new RapportStatistiqueDto(
                0, 0, 0, 0, statutsVides, 0, 0, List.of(), List.of(), List.of(), 0, List.of());

        assertThat(service.genererRapport(rapport)).isNotEmpty();
    }
}
