package inpg.sn.lms.recrutement.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import inpg.sn.lms.pdf.RapportPdf;

/**
 * Rapports de synthèse du recrutement (candidatures, tests, entretiens), en
 * PDF : indicateurs clés + répartitions chiffrées, avec le choix Excel côté
 * {@code AnalyticsController}. La mise en page (logo INPG, pied de page, pagination) est celle de {@link RapportPdf}.
 *
 * <p>Distinct de {@link RapportRecrutementPdfService} (rapport qualité
 * nominatif par étape, demandé séparément) : celui-ci ne liste pas les
 * candidats, il n'affiche que les chiffres agrégés déjà calculés par
 * {@code AnalyticsController}, jamais recalculés ici.
 */
@Service
public class RapportSyntheseRecrutementPdfService {

    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] genererRapport(String titre, List<LigneRapport> lignes) {
        try (RapportPdf pdf = new RapportPdf(titre)) {
            pdf.titre(titre);
            pdf.meta("Institut National du Pétrole et du Gaz");
            pdf.meta("Généré le " + LocalDateTime.now().format(FORMAT_DATE));
            pdf.espace(10);

            Map<String, List<LigneRapport>> parSection = new LinkedHashMap<>();
            for (LigneRapport ligne : lignes) {
                parSection.computeIfAbsent(ligne.section(), s -> new ArrayList<>()).add(ligne);
            }
            for (Map.Entry<String, List<LigneRapport>> section : parSection.entrySet()) {
                pdf.section(section.getKey());
                for (LigneRapport ligne : section.getValue()) {
                    pdf.ligneCle(ligne.libelle(), ligne.valeur());
                }
                pdf.espace(6);
            }
            return pdf.generer();
        } catch (IOException | RuntimeException exception) {
            throw new RuntimeException("La génération du rapport a échoué.", exception);
        }
    }
}
