package inpg.sn.lms.espace_etudiant.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;

import inpg.sn.lms.espace_etudiant.dto.QualiteCoursDto;
import inpg.sn.lms.espace_etudiant.dto.SyntheseEnqueteSuiviDto;
import inpg.sn.lms.pdf.RapportPdf;

/**
 * Rapports du suivi qualité, en PDF (TDR §G, besoin B5).
 *
 * <p>Deux rapports <strong>distincts</strong>, chacun téléchargeable
 * séparément : les avis des étudiants sur les cours d'une part, l'impact de la
 * formation sur la carrière des diplômés d'autre part. Ce sont deux lectures,
 * deux publics et deux temporalités — les mélanger dans un seul document rendait
 * chacun moins exploitable.
 *
 * <p>Aucun chiffre n'est recalculé ici : le service ne fait que mettre en page
 * les {@link QualiteCoursDto} / {@link SyntheseEnqueteSuiviDto} déjà produits
 * pour l'écran « Suivi qualité », pour ne jamais en diverger. La mise en page
 * (logo INPG, pied de page, pagination, retour à la ligne) est celle de
 * {@link RapportPdf} : aucun commentaire n'est tronqué.
 */
@Service
public class RapportQualitePdfService {

    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Suffixe de date pour les noms de fichiers : {@code AAAA-MM-JJ}. */
    public String suffixeDuJour() {
        return LocalDate.now().toString();
    }

    // ======================================================================
    // Rapport 1 — Avis des étudiants sur les cours
    // ======================================================================

    public byte[] genererRapportAvisCours(QualiteCoursDto qualite) {
        try (RapportPdf pdf = new RapportPdf("Rapport qualité — Avis sur les cours")) {
            enTete(pdf, "Rapport qualité — Avis sur les cours");

            pdf.section("Chiffres clés");
            pdf.ligneCle("Moyenne générale",
                    qualite.moyenneGenerale() == null ? "aucun avis" : qualite.moyenneGenerale() + " / 5");
            pdf.ligneCle("Nombre total d'avis déposés", String.valueOf(qualite.nombreAvis()));
            pdf.ligneCle("Cours évalués", String.valueOf(qualite.coursEvalues()));
            pdf.ligneCle("Cours à surveiller (moyenne ≤ 3 / 5)", String.valueOf(qualite.coursASurveiller()));
            pdf.espace(8);

            pdf.section("Détail par cours (du moins bien noté au mieux noté)");
            if (qualite.parCours().isEmpty()) {
                pdf.texteItalique("Aucun avis n'a encore été déposé.");
            } else {
                for (QualiteCoursDto.LigneQualite ligne : qualite.parCours()) {
                    String moyenne = ligne.moyenne() == null ? "-" : ligne.moyenne() + " / 5";
                    pdf.puce(intitule(ligne));
                    pdf.detail("Moyenne " + moyenne
                            + "  |  " + ligne.nombreAvis() + " avis"
                            + "  |  " + ligne.actionsRequises() + " avis ≤ 3 / 5");
                    for (String commentaire : ligne.derniersCommentaires()) {
                        pdf.detailItalique("« " + commentaire + " »");
                    }
                    pdf.espace(5);
                }
            }
            pdf.espace(8);

            pdf.section("Cours à surveiller");
            List<QualiteCoursDto.LigneQualite> aSurveiller = qualite.parCours().stream()
                    .filter(l -> l.moyenne() != null && l.moyenne() <= 3)
                    .toList();
            if (aSurveiller.isEmpty()) {
                pdf.texteItalique("Aucun cours sous le seuil d'alerte. Rien ne requiert d'action immédiate.");
            } else {
                pdf.texte("Ces cours ont une moyenne d'au plus 3 / 5 : ils appellent un échange avec l'équipe pédagogique.");
                for (QualiteCoursDto.LigneQualite ligne : aSurveiller) {
                    pdf.puce(intitule(ligne) + " : " + ligne.moyenne() + " / 5 (" + ligne.nombreAvis() + " avis)");
                    for (String commentaire : ligne.derniersCommentaires()) {
                        pdf.detailItalique("« " + commentaire + " »");
                    }
                    pdf.espace(3);
                }
            }

            return pdf.generer();
        } catch (IOException | RuntimeException exception) {
            throw new RuntimeException("La generation du rapport sur les avis a echoue.", exception);
        }
    }

    // ======================================================================
    // Rapport 2 — Enquêtes de suivi de carrière
    // ======================================================================

    public byte[] genererRapportEnquetes(List<SyntheseEnqueteSuiviDto> enquetes) {
        try (RapportPdf pdf = new RapportPdf("Rapport qualité — Enquêtes de suivi de carrière")) {
            enTete(pdf, "Rapport qualité — Enquêtes de suivi de carrière");

            int totalReponses = enquetes.stream().mapToInt(SyntheseEnqueteSuiviDto::nombreReponses).sum();
            long actives = enquetes.stream().filter(SyntheseEnqueteSuiviDto::active).count();
            pdf.section("Chiffres clés");
            pdf.ligneCle("Campagnes lancées", String.valueOf(enquetes.size()));
            pdf.ligneCle("Campagnes actives", String.valueOf(actives));
            pdf.ligneCle("Total des réponses recueillies", String.valueOf(totalReponses));
            pdf.espace(8);

            pdf.section("Détail par campagne");
            if (enquetes.isEmpty()) {
                pdf.texteItalique("Aucune campagne d'enquête n'a encore été lancée auprès des alumni.");
            } else {
                for (SyntheseEnqueteSuiviDto enquete : enquetes) {
                    pdf.puce(enquete.titre() + "  (" + (enquete.active() ? "active" : "clôturée") + ")");
                    if (enquete.description() != null && !enquete.description().isBlank()) {
                        pdf.detail(enquete.description());
                    }
                    if (enquete.dateCreation() != null) {
                        pdf.detail("Lancée le " + enquete.dateCreation().format(FORMAT_DATE));
                    }
                    pdf.detail("Réponses : " + enquete.nombreReponses()
                            + (enquete.nombreDestinataires() > 0
                                    ? " / " + enquete.nombreDestinataires() + " alumni (" + tauxReponse(enquete) + ")"
                                    : ""));
                    pdf.detail("Application des compétences : "
                            + (enquete.moyenneApplicationCompetences() == null ? "-"
                                    : enquete.moyenneApplicationCompetences() + " / 5")
                            + "   |   Impact sur la carrière : "
                            + (enquete.moyenneImpactCarriere() == null ? "-"
                                    : enquete.moyenneImpactCarriere() + " / 5"));
                    for (String commentaire : enquete.derniersCommentaires()) {
                        pdf.detailItalique("« " + commentaire + " »");
                    }
                    pdf.espace(6);
                }
            }

            return pdf.generer();
        } catch (IOException | RuntimeException exception) {
            throw new RuntimeException("La generation du rapport sur les enquetes a echoue.", exception);
        }
    }

    private void enTete(RapportPdf pdf, String titre) throws IOException {
        pdf.titre(titre);
        pdf.meta("Institut National du Pétrole et du Gaz");
        pdf.meta("Généré le " + LocalDateTime.now().format(FORMAT_DATE));
        pdf.espace(10);
    }

    private String intitule(QualiteCoursDto.LigneQualite ligne) {
        return (ligne.coursCode() == null || ligne.coursCode().isBlank() ? "" : ligne.coursCode() + " - ")
                + ligne.coursTitre();
    }

    private String tauxReponse(SyntheseEnqueteSuiviDto enquete) {
        if (enquete.nombreDestinataires() <= 0) {
            return "-";
        }
        return Math.round(100.0 * enquete.nombreReponses() / enquete.nombreDestinataires()) + " %";
    }
}
