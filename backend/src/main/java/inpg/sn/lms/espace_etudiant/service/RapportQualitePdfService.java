package inpg.sn.lms.espace_etudiant.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import inpg.sn.lms.espace_etudiant.dto.QualiteCoursDto;
import inpg.sn.lms.espace_etudiant.dto.SyntheseEnqueteSuiviDto;

import java.util.List;

/**
 * Rapport qualité global, en PDF (TDR §G, besoin B5 : « Intégration des
 * résultats dans un rapport qualité global »).
 *
 * <p>Consolide deux sources déjà calculées ailleurs — {@link QualiteCoursDto}
 * (avis de cours) et {@link SyntheseEnqueteSuiviDto} (enquêtes de suivi) —
 * sans recalculer : ce service ne fait que les mettre en page, pour ne jamais
 * diverger des chiffres déjà affichés à l'écran « Suivi qualité ». Même
 * bibliothèque (PDFBox) que {@code CertificatPdfService}, patron de mise en
 * page volontairement plus simple (rapport interne, pas un document officiel
 * remis à un tiers).
 */
@Service
public class RapportQualitePdfService {

    private static final float MARGE = 50f;
    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] genererPdf(QualiteCoursDto qualiteCours, List<SyntheseEnqueteSuiviDto> enquetes) {
        try (PDDocument document = new PDDocument()) {
            Contexte ctx = new Contexte(document);

            ctx.titre("Rapport Qualite - INPG LMS");
            ctx.texte(ctx.italique, 10,
                    "Genere le " + LocalDateTime.now().format(FORMAT_DATE));
            ctx.espace(20);

            ctx.sousTitre("1. Avis sur les cours");
            ctx.ligneKpi("Moyenne generale",
                    qualiteCours.moyenneGenerale() == null ? "-" : qualiteCours.moyenneGenerale() + " / 5");
            ctx.ligneKpi("Nombre total d'avis", String.valueOf(qualiteCours.nombreAvis()));
            ctx.ligneKpi("Cours evalues", String.valueOf(qualiteCours.coursEvalues()));
            ctx.ligneKpi("Cours a surveiller (<= 3/5)", String.valueOf(qualiteCours.coursASurveiller()));
            ctx.espace(10);

            if (qualiteCours.parCours().isEmpty()) {
                ctx.texte(ctx.italique, 10, "Aucun avis depose pour l'instant.");
            } else {
                for (QualiteCoursDto.LigneQualite ligne : qualiteCours.parCours()) {
                    String moyenne = ligne.moyenne() == null ? "-" : ligne.moyenne() + " / 5";
                    ctx.ligneListe((ligne.coursCode() == null ? "" : ligne.coursCode() + " - ") + ligne.coursTitre()
                            + " : " + moyenne + " (" + ligne.nombreAvis() + " avis, "
                            + ligne.actionsRequises() + " a surveiller)");
                }
            }

            ctx.espace(20);
            ctx.sousTitre("2. Enquetes de suivi de carriere");
            if (enquetes.isEmpty()) {
                ctx.texte(ctx.italique, 10, "Aucune campagne lancee pour l'instant.");
            } else {
                for (SyntheseEnqueteSuiviDto enquete : enquetes) {
                    String etat = enquete.active() ? "active" : "cloturee";
                    ctx.ligneListe(enquete.titre() + " (" + etat + ") : " + enquete.nombreReponses() + " reponse(s)"
                            + " - application des competences "
                            + (enquete.moyenneApplicationCompetences() == null ? "-"
                                    : enquete.moyenneApplicationCompetences() + "/5")
                            + " - impact carriere "
                            + (enquete.moyenneImpactCarriere() == null ? "-" : enquete.moyenneImpactCarriere() + "/5"));
                }
            }

            ctx.fermer();
            ByteArrayOutputStream sortie = new ByteArrayOutputStream();
            document.save(sortie);
            return sortie.toByteArray();
        } catch (Exception exception) {
            throw new RuntimeException("La generation du rapport qualite a echoue.", exception);
        }
    }

    /** Petite machine à écrire verticale avec saut de page automatique. */
    private static final class Contexte {
        private final PDDocument document;
        private PDPage page;
        private PDPageContentStream content;
        private float y;
        private final float largeurUtile;

        final PDType1Font normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        final PDType1Font gras = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        final PDType1Font italique = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

        Contexte(PDDocument document) throws IOException {
            this.document = document;
            nouvellePage();
            this.largeurUtile = PDRectangle.A4.getWidth() - 2 * MARGE;
        }

        private void nouvellePage() throws IOException {
            if (content != null) {
                content.close();
            }
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            content = new PDPageContentStream(document, page);
            y = PDRectangle.A4.getHeight() - MARGE;
        }

        private void assurerEspace(float hauteurLigne) throws IOException {
            if (y - hauteurLigne < MARGE) {
                nouvellePage();
            }
        }

        void titre(String texte) throws IOException {
            assurerEspace(30);
            content.setNonStrokingColor(new Color(19, 23, 65));
            ecrire(gras, 20, texte);
            content.setNonStrokingColor(Color.BLACK);
            y -= 8;
        }

        void sousTitre(String texte) throws IOException {
            espace(6);
            assurerEspace(22);
            content.setStrokingColor(new Color(19, 23, 65));
            content.setLineWidth(1f);
            content.moveTo(MARGE, y + 4);
            content.lineTo(MARGE + largeurUtile, y + 4);
            content.stroke();
            ecrire(gras, 14, texte);
        }

        void ligneKpi(String libelle, String valeur) throws IOException {
            assurerEspace(16);
            ecrire(normal, 11, libelle + " : " + valeur);
        }

        void ligneListe(String texte) throws IOException {
            assurerEspace(16);
            ecrire(normal, 10, "- " + texte);
        }

        void texte(PDType1Font font, float taille, String texte) throws IOException {
            assurerEspace(16);
            ecrire(font, taille, texte);
        }

        void espace(float hauteur) {
            y -= hauteur;
        }

        private void ecrire(PDType1Font font, float taille, String texte) throws IOException {
            content.beginText();
            content.setFont(font, taille);
            content.newLineAtOffset(MARGE, y);
            content.showText(tronquer(texte, font, taille, largeurUtile));
            content.endText();
            y -= taille + 6;
        }

        /** Coupe le texte plutot que de le laisser deborder de la page. */
        private String tronquer(String texte, PDType1Font font, float taille, float largeurMax) throws IOException {
            if (font.getStringWidth(texte) / 1000 * taille <= largeurMax) {
                return texte;
            }
            String suffixe = "...";
            StringBuilder resultat = new StringBuilder();
            for (char c : texte.toCharArray()) {
                String essai = resultat.toString() + c + suffixe;
                if (font.getStringWidth(essai) / 1000 * taille > largeurMax) {
                    break;
                }
                resultat.append(c);
            }
            return resultat + suffixe;
        }

        void fermer() throws IOException {
            content.close();
        }
    }
}
