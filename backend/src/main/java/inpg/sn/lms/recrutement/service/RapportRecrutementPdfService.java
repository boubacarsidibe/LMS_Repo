package inpg.sn.lms.recrutement.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import inpg.sn.lms.pdf.RapportPdf;
import inpg.sn.lms.recrutement.dto.RapportRecrutementDto;

/**
 * Rapport qualité du recrutement, en PDF : répartition des candidatures par
 * étape du workflow avec la liste nominative de chacune (TDR §A).
 *
 * <p>Aucun chiffre n'est recalculé ici : le service met seulement en page le
 * {@link RapportRecrutementDto} déjà produit par
 * {@code CandidatureService#genererRapportParEtape}. La mise en page (logo INPG, pied de page, pagination, tableau,
 * retour à la ligne) est celle de {@link RapportPdf}, commune à tous les rapports PDF de l'institut.
 */
@Service
public class RapportRecrutementPdfService {

    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMAT_DATE_HEURE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Suffixe de date pour le nom de fichier : {@code AAAA-MM-JJ}. */
    public String suffixeDuJour() {
        return LocalDate.now().toString();
    }

    public byte[] genererRapport(RapportRecrutementDto rapport) {
        try (RapportPdf pdf = new RapportPdf("Rapport qualité — Recrutement")) {
            pdf.titre("Rapport qualité — Recrutement");
            pdf.meta("Institut National du Pétrole et du Gaz");
            pdf.meta("Généré le " + LocalDateTime.now().format(FORMAT_DATE_HEURE));
            pdf.espace(10);

            pdf.section("Chiffres clés");
            pdf.tuiles(new String[][] {
                    { String.valueOf(rapport.totalCandidatures()), "Candidature(s) au total, toutes sessions confondues" },
            });
            pdf.espace(6);

            List<String[]> lignes = new ArrayList<>();
            for (RapportRecrutementDto.EtapeRapport etape : rapport.etapes()) {
                lignes.add(new String[] { etape.libelle(),
                        etape.nombre() + tauxEntreParentheses(etape.nombre(), rapport.totalCandidatures()) });
            }
            pdf.tableau(new String[] { "Étape du workflow", "Candidatures" }, new float[] { 4, 1.6f }, lignes);
            pdf.espace(10);

            for (RapportRecrutementDto.EtapeRapport etape : rapport.etapes()) {
                pdf.section(etape.libelle() + " (" + etape.nombre() + ")");
                if (etape.candidats().isEmpty()) {
                    pdf.texteItalique("Aucune candidature à cette étape.");
                    pdf.espace(6);
                    continue;
                }
                for (RapportRecrutementDto.CandidatRapport c : etape.candidats()) {
                    pdf.puce((c.numeroDossier() == null ? "" : c.numeroDossier() + " — ")
                            + nvl(c.nom()) + " " + nvl(c.prenom()));
                    pdf.detail(nvl(c.email())
                            + (c.sessionLibelle() != null ? "  |  Session : " + c.sessionLibelle() : "")
                            + (c.dateSoumission() != null ? "  |  Déposé le " + c.dateSoumission().format(FORMAT_DATE) : ""));
                    if (c.motifRejet() != null && !c.motifRejet().isBlank()) {
                        pdf.detail("Motif du rejet : " + c.motifRejet());
                    }
                    pdf.espace(3);
                }
                pdf.espace(6);
            }

            return pdf.generer();
        } catch (IOException | RuntimeException exception) {
            throw new RuntimeException("La génération du rapport qualité recrutement a échoué.", exception);
        }
    }

    private String nvl(String valeur) {
        return valeur == null ? "" : valeur;
    }

    private String tauxEntreParentheses(int nombre, int total) {
        if (total <= 0) {
            return "";
        }
        return " (" + Math.round(100.0 * nombre / total) + " %)";
    }
}
