package inpg.sn.lms.espace_etudiant.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import inpg.sn.lms.espace_etudiant.dto.QualiteCoursDto;
import inpg.sn.lms.espace_etudiant.dto.SyntheseEnqueteSuiviDto;

/**
 * {@link RapportQualitePdfService} : deux rapports distincts, chacun généré
 * sans recalcul et robuste à un titre de cours / d'enquête hors WinAnsi.
 */
class RapportQualitePdfServiceTest {

    private final RapportQualitePdfService service = new RapportQualitePdfService();

    @Test
    @DisplayName("rapport « avis sur les cours » : PDF valide, y compris avec un titre exotique")
    void rapport_avis_cours_valide() {
        QualiteCoursDto.LigneQualite ligne = new QualiteCoursDto.LigneQualite(
                1L, "GEO-101", "Géologie 🛢", 2.5, 4, 3, List.of("à revoir", "trop rapide"));
        QualiteCoursDto qualite = new QualiteCoursDto(3.2, 12, 3, 1, List.of(ligne));

        byte[] pdf = service.genererRapportAvisCours(qualite);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("rapport « enquêtes de suivi » : PDF valide avec taux de réponse et sans campagne")
    void rapport_enquetes_valide() {
        SyntheseEnqueteSuiviDto enquete = new SyntheseEnqueteSuiviDto(
                7L, "Suivi des diplômés 2026", "Impact carrière", LocalDateTime.now(), true,
                8, 20, 4.1, 3.7, List.of("très utile"));

        assertThat(service.genererRapportEnquetes(List.of(enquete))).isNotEmpty();
        assertThat(service.genererRapportEnquetes(List.of())).isNotEmpty();
    }
}
