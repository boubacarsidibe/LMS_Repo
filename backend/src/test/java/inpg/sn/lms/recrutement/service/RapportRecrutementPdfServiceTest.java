package inpg.sn.lms.recrutement.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import inpg.sn.lms.recrutement.dto.RapportRecrutementDto;

/**
 * {@link RapportRecrutementPdfService} : mise en page du rapport qualité
 * recrutement, sans recalcul, robuste à une étape vide et à un nom hors
 * WinAnsi.
 */
class RapportRecrutementPdfServiceTest {

    private final RapportRecrutementPdfService service = new RapportRecrutementPdfService();

    @Test
    @DisplayName("PDF valide avec des candidats répartis sur plusieurs étapes, dont un motif de rejet")
    void rapport_valide_avec_plusieurs_etapes() {
        RapportRecrutementDto.CandidatRapport soumis = new RapportRecrutementDto.CandidatRapport(
                "INPG-0001", "Ndiaye", "Awa 🎓", "awa@example.com", "Rentrée 2026", LocalDateTime.now(), null);
        RapportRecrutementDto.CandidatRapport rejete = new RapportRecrutementDto.CandidatRapport(
                "INPG-0002", "Diop", "Cheikh", "cheikh@example.com", "Rentrée 2026", LocalDateTime.now(),
                "Critères de présélection non remplis");

        RapportRecrutementDto rapport = new RapportRecrutementDto(2, List.of(
                new RapportRecrutementDto.EtapeRapport("SOUMIS", "Soumis", 1, List.of(soumis)),
                new RapportRecrutementDto.EtapeRapport("EN_COURS_DE_PRESELECTION", "En présélection", 0, List.of()),
                new RapportRecrutementDto.EtapeRapport("ADMIS1", "Admis au test académique", 0, List.of()),
                new RapportRecrutementDto.EtapeRapport("ADMIS2", "Admis à l'entretien", 0, List.of()),
                new RapportRecrutementDto.EtapeRapport("ADMIS3", "Admis (décision finale)", 0, List.of()),
                new RapportRecrutementDto.EtapeRapport("REJETE", "Rejetés", 1, List.of(rejete))));

        byte[] pdf = service.genererRapport(rapport);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("Aucune candidature : PDF valide malgré des étapes toutes vides")
    void rapport_valide_sans_aucune_candidature() {
        List<RapportRecrutementDto.EtapeRapport> etapes = List.of(
                new RapportRecrutementDto.EtapeRapport("SOUMIS", "Soumis", 0, List.of()),
                new RapportRecrutementDto.EtapeRapport("EN_COURS_DE_PRESELECTION", "En présélection", 0, List.of()),
                new RapportRecrutementDto.EtapeRapport("ADMIS1", "Admis au test académique", 0, List.of()),
                new RapportRecrutementDto.EtapeRapport("ADMIS2", "Admis à l'entretien", 0, List.of()),
                new RapportRecrutementDto.EtapeRapport("ADMIS3", "Admis (décision finale)", 0, List.of()),
                new RapportRecrutementDto.EtapeRapport("REJETE", "Rejetés", 0, List.of()));

        assertThat(service.genererRapport(new RapportRecrutementDto(0, etapes))).isNotEmpty();
    }
}
