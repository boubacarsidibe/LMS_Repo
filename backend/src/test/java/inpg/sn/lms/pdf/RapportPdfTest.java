package inpg.sn.lms.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Le moteur des rapports PDF et le papier à en-tête. Les tests lisent le PDF produit — texte, positions, traits
 * dessinés — plutôt que de constater seulement qu'un fichier est généré : c'est exactement ce que les anciens
 * rapports ne permettaient pas de voir (titres « barrés » par leur filet, textes coupés par « ... »).
 */
class RapportPdfTest {

    private static final String LONG = "Session de formation interne sur la sécurité des installations pétrolières et gazières : "
            + "présentation des procédures d'urgence, exercices pratiques en atelier, retours d'expérience des opérateurs de "
            + "terrain, évaluation des acquis par quiz et débriefing collectif. Prévoir la mise à jour du support avant la "
            + "prochaine session prévue en octobre, avec un budget de 1 200 € par session et un maximum de douze participants "
            + "par groupe. Dernier mot du commentaire : TERMINUS.";

    private byte[] rapportDeTest(int elements) throws IOException {
        try (RapportPdf pdf = new RapportPdf("Rapport de test")) {
            pdf.titre("Rapport de test — mise en page");
            pdf.meta("Institut National du Pétrole et du Gaz");
            pdf.meta("Généré le 25/09/2026");
            pdf.espace(10);
            pdf.tuiles(new String[][] { { "2", "Formateurs" }, { "0", "Sans cours affecté" } });
            pdf.section("Chiffres clés");
            pdf.ligneCle("Moyenne générale", "3.4 / 5");
            for (int i = 1; i <= elements; i++) {
                pdf.section("Section numéro " + i);
                pdf.puce("Élément " + i + " — titre assez long pour vérifier le retour à la ligne d'un intitulé de puce");
                pdf.detail(LONG);
                pdf.detailItalique("« " + LONG + " »");
                pdf.detailAlerte("Aucun cours affecté — invisible dans son propre espace.");
            }
            return pdf.generer();
        }
    }

    private String texte(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document).replaceAll("\\s+", " ");
        }
    }

    @Test
    @DisplayName("un texte long passe à la ligne et rien n'est perdu : le dernier mot est bien dans le PDF")
    void rien_n_est_tronque() throws IOException {
        String texte = texte(rapportDeTest(1));

        assertThat(texte).contains("TERMINUS.").doesNotContain("...");
    }

    @Test
    @DisplayName("un mot plus large qu'une ligne est coupé au caractère au lieu de déborder de la page")
    void mot_geant_coupe() throws IOException {
        try (RapportPdf pdf = new RapportPdf("Test")) {
            pdf.detail("A".repeat(400));
            byte[] octets = pdf.generer();

            assertThat(texte(octets)).contains("AAAA");
            assertThat(positions(octets)).allSatisfy(p -> assertThat(p.getX() + p.getWidth())
                    .isLessThanOrEqualTo(595.3f - PapierEnTete.MARGE_DROITE + 1f));
        }
    }

    @Test
    @DisplayName("aucun filet ne traverse un texte : chaque trait est au-dessus ou au-dessous des glyphes voisins")
    void aucun_filet_ne_barre_un_texte() throws IOException {
        byte[] pdf = rapportDeTest(6);

        try (PDDocument document = Loader.loadPDF(pdf)) {
            for (int numero = 0; numero < document.getNumberOfPages(); numero++) {
                PDPage page = document.getPage(numero);
                float hauteur = page.getMediaBox().getHeight();
                Capteur capteur = new Capteur(page);
                capteur.processPage(page);
                List<TextPosition> textes = positions(pdf, numero + 1);

                assertThat(capteur.traitsHorizontaux).as("la page %d porte des filets de section", numero + 1).isNotNull();
                for (float[] trait : capteur.traitsHorizontaux) { // {x1, x2, y}
                    if (trait[2] < PapierEnTete.BAS_CONTENU) {
                        continue; // soulignement voulu du lien du pied de page, hors du contenu
                    }
                    for (TextPosition t : textes) {
                        float base = hauteur - t.getYDirAdj();
                        float bas = base - 0.25f * t.getFontSizeInPt();
                        float haut = base + 0.75f * t.getFontSizeInPt();
                        boolean memeColonne = t.getX() < trait[1] && t.getX() + t.getWidth() > trait[0];
                        if (memeColonne) {
                            assertThat(trait[2] > haut || trait[2] < bas)
                                    .as("page %d : filet à y=%.1f coupe « %s » (glyphes de %.1f à %.1f)",
                                            numero + 1, trait[2], t.getUnicode(), bas, haut)
                                    .isTrue();
                        }
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("chaque page porte le logo, le pied de page (adresse, téléphone, site cliquable) et sa pagination")
    void papier_a_en_tete_sur_chaque_page() throws IOException {
        byte[] pdf = rapportDeTest(8);

        try (PDDocument document = Loader.loadPDF(pdf)) {
            int total = document.getNumberOfPages();
            assertThat(total).isGreaterThan(1);
            for (int numero = 1; numero <= total; numero++) {
                PDPage page = document.getPage(numero - 1);
                Capteur capteur = new Capteur(page);
                capteur.processPage(page);
                assertThat(capteur.images).as("logo + deux icônes, page %d", numero).isGreaterThanOrEqualTo(3);

                PDFTextStripper extracteur = new PDFTextStripper();
                extracteur.setStartPage(numero);
                extracteur.setEndPage(numero);
                String texte = extracteur.getText(document).replaceAll("\\s+", " ");
                assertThat(texte)
                        .contains("Immeuble Adja Koone - Boulevard de l'Est, Point E, Dakar – Sénégal")
                        .contains("+221 33 865 91 48 / 49")
                        .contains("www.inpg.sn")
                        .contains("Page " + numero + " / " + total);

                List<String> liens = new ArrayList<>();
                for (PDAnnotation annotation : page.getAnnotations()) {
                    if (annotation instanceof PDAnnotationLink lien && lien.getAction() instanceof PDActionURI uri) {
                        liens.add(uri.getURI());
                    }
                }
                assertThat(liens).containsExactly("https://www.inpg.sn");
            }
        }
    }

    @Test
    @DisplayName("le contenu reste dans les marges : rien ne se glisse sous le logo ni sur le pied de page")
    void le_contenu_respecte_les_marges() throws IOException {
        byte[] pdf = rapportDeTest(4);

        try (PDDocument document = Loader.loadPDF(pdf)) {
            for (int numero = 1; numero <= document.getNumberOfPages(); numero++) {
                float hauteur = document.getPage(numero - 1).getMediaBox().getHeight();
                for (TextPosition t : positions(pdf, numero)) {
                    float base = hauteur - t.getYDirAdj();
                    String texte = t.getUnicode();
                    boolean papier = base < PapierEnTete.BAS_CONTENU; // pagination et pied de page
                    if (!papier) {
                        assertThat(base + 0.75f * t.getFontSizeInPt())
                                .as("« %s » sous le logo", texte)
                                .isLessThanOrEqualTo(hauteur - PapierEnTete.HAUT_CONTENU + 2f);
                        assertThat(t.getX()).as("« %s » dans la marge gauche", texte)
                                .isGreaterThanOrEqualTo(PapierEnTete.MARGE_GAUCHE - 1f);
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("un caractère hors de la police devient « ? » sans faire échouer la génération")
    void caractere_inconnu_tolere() throws IOException {
        try (RapportPdf pdf = new RapportPdf("Test")) {
            pdf.puce("Forage 🛢 profond 李");

            assertThat(texte(pdf.generer())).contains("Forage ? profond ?");
        }
    }

    // ===================== Outils =====================

    private List<TextPosition> positions(byte[] pdf) throws IOException {
        return positions(pdf, 1);
    }

    private List<TextPosition> positions(byte[] pdf, int page) throws IOException {
        List<TextPosition> resultat = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDFTextStripper extracteur = new PDFTextStripper() {
                @Override
                protected void processTextPosition(TextPosition texte) {
                    if (!texte.getUnicode().isBlank()) {
                        resultat.add(texte);
                    }
                    super.processTextPosition(texte);
                }
            };
            extracteur.setStartPage(page);
            extracteur.setEndPage(page);
            extracteur.getText(document);
        }
        return resultat;
    }

    /** Relève les traits horizontaux réellement tracés (filets) et compte les images d'une page. */
    private static final class Capteur extends PDFGraphicsStreamEngine {
        final List<float[]> traitsHorizontaux = new ArrayList<>();
        int images;
        private final List<float[]> enCours = new ArrayList<>();
        private float x0, y0;

        Capteur(PDPage page) {
            super(page);
        }

        @Override
        public void moveTo(float x, float y) {
            x0 = x;
            y0 = y;
        }

        @Override
        public void lineTo(float x, float y) {
            if (Math.abs(y - y0) < 0.01f) {
                enCours.add(new float[] { Math.min(x0, x), Math.max(x0, x), y });
            }
            x0 = x;
            y0 = y;
        }

        @Override
        public void strokePath() {
            traitsHorizontaux.addAll(enCours);
            enCours.clear();
        }

        @Override
        public void endPath() {
            enCours.clear();
        }

        @Override
        public void fillPath(int regle) {
            enCours.clear();
        }

        @Override
        public void fillAndStrokePath(int regle) {
            enCours.clear();
        }

        @Override
        public void drawImage(PDImage image) {
            images++;
        }

        @Override
        public void appendRectangle(Point2D p0, Point2D p1, Point2D p2, Point2D p3) {
        }

        @Override
        public void clip(int regle) {
        }

        @Override
        public void curveTo(float x1, float y1, float x2, float y2, float x3, float y3) {
        }

        @Override
        public Point2D getCurrentPoint() {
            return new Point2D.Float(x0, y0);
        }

        @Override
        public void closePath() {
        }

        @Override
        public void shadingFill(COSName nom) {
        }
    }
}
