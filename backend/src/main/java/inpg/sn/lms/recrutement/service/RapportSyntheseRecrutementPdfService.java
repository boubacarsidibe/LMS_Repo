package inpg.sn.lms.recrutement.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

/**
 * Rapports de synthèse du recrutement (candidatures, tests, entretiens), en
 * PDF : indicateurs clés + répartitions chiffrées, avec le choix Excel côté
 * {@code AnalyticsController}. Même bibliothèque et même patron
 * {@code Contexte} que {@code RapportQualitePdfService} /
 * {@code RapportActivitePdfService}, dupliqué plutôt que partagé — même
 * convention que ces deux services.
 *
 * <p>Distinct de {@link RapportRecrutementPdfService} (rapport qualité
 * nominatif par étape, demandé séparément) : celui-ci ne liste pas les
 * candidats, il n'affiche que les chiffres agrégés déjà calculés par
 * {@code AnalyticsController}, jamais recalculés ici.
 */
@Service
public class RapportSyntheseRecrutementPdfService {

    private static final float MARGE = 50f;
    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] genererRapport(String titre, List<LigneRapport> lignes) {
        try (PDDocument document = new PDDocument()) {
            Contexte ctx = new Contexte(document);

            ctx.titre(titre);
            ctx.texte(ctx.italique, 10, "Généré le " + LocalDateTime.now().format(FORMAT_DATE));
            ctx.espace(14);

            Map<String, List<LigneRapport>> parSection = new LinkedHashMap<>();
            for (LigneRapport ligne : lignes) {
                parSection.computeIfAbsent(ligne.section(), s -> new java.util.ArrayList<>()).add(ligne);
            }
            for (Map.Entry<String, List<LigneRapport>> section : parSection.entrySet()) {
                ctx.sousTitre(section.getKey());
                for (LigneRapport ligne : section.getValue()) {
                    ctx.ligneKpi(ligne.libelle(), ligne.valeur());
                }
                ctx.espace(6);
            }

            ctx.fermer();
            ByteArrayOutputStream sortie = new ByteArrayOutputStream();
            document.save(sortie);
            return sortie.toByteArray();
        } catch (Exception exception) {
            throw new RuntimeException("La génération du rapport a échoué.", exception);
        }
    }

    /** Petite machine à écrire verticale avec saut de page automatique — identique à RapportQualitePdfService. */
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
