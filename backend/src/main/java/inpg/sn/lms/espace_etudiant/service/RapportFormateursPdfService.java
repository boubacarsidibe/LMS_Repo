package inpg.sn.lms.espace_etudiant.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

import inpg.sn.lms.espace_etudiant.dto.RapportFormateursDto;
import inpg.sn.lms.pdf.RapportPdf;

/**
 * Rapport des comptes formateurs, en PDF (demande responsable pédagogique) :
 * effectif complet avec coordonnées et cours affectés, en signalant
 * distinctement les formateurs sans aucun cours.
 *
 * <p>Aucun chiffre n'est recalculé ici : le service met seulement en page le
 * {@link RapportFormateursDto} déjà produit par
 * {@code PedagogieController#construireRapportFormateurs}. La mise en page (logo INPG, pied de page, pagination,
 * retour à la ligne) est celle de {@link RapportPdf}.
 */
@Service
public class RapportFormateursPdfService {

    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMAT_DATE_HEURE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] genererRapport(RapportFormateursDto rapport) {
        try (RapportPdf pdf = new RapportPdf("Rapport des comptes formateurs")) {
            pdf.titre("Rapport des comptes formateurs");
            pdf.meta("Institut National du Pétrole et du Gaz");
            pdf.meta("Généré le " + LocalDateTime.now().format(FORMAT_DATE_HEURE));
            pdf.espace(14);

            pdf.tuiles(new String[][] {
                    { String.valueOf(rapport.totalFormateurs()), "Formateurs" },
                    { String.valueOf(rapport.formateursSansCours()), "Sans cours affecté" },
            });
            pdf.espace(6);

            pdf.section("Effectif");
            if (rapport.formateurs().isEmpty()) {
                pdf.texteItalique("Aucun compte formateur pour le moment.");
            }
            for (RapportFormateursDto.FormateurRapport f : rapport.formateurs()) {
                pdf.puce((nvl(f.prenom()) + " " + nvl(f.nom())).trim()
                        + (f.identifiant() != null ? " (" + f.identifiant() + ")" : ""));
                pdf.detail(nvl(f.email())
                        + (f.telephone() != null ? "  |  " + f.telephone() : "")
                        + (f.dateCreationCompte() != null ? "  |  Compte créé le " + f.dateCreationCompte().format(FORMAT_DATE) : ""));
                if (f.coursEnseignes().isEmpty()) {
                    pdf.detailAlerte("Aucun cours affecté — invisible dans son propre espace formateur.");
                } else {
                    pdf.detail("Cours : " + String.join(", ", f.coursEnseignes()));
                }
                pdf.espace(3);
            }

            return pdf.generer();
        } catch (IOException | RuntimeException exception) {
            throw new RuntimeException("La génération du rapport des comptes formateurs a échoué.", exception);
        }
    }

    private String nvl(String valeur) {
        return valeur == null ? "" : valeur;
    }
}
