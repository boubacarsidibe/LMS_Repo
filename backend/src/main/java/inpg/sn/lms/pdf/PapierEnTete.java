package inpg.sn.lms.pdf;

import java.awt.Color;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary;

/**
 * Papier à en-tête de l'INPG : logo officiel en haut à gauche, pied de page (adresse, téléphone, site) centré, et
 * pagination. Mesures relevées sur « En tete et bas de page document.docx » (voir {@code pdf/README.md}).
 *
 * <p>S'applique à n'importe quel {@link PDDocument}, une fois son contenu écrit : les générateurs doivent seulement
 * laisser libres les marges {@link #HAUT_CONTENU} en haut et {@link #BAS_CONTENU} en bas, où le papier s'imprime.
 */
public final class PapierEnTete {

    /** Marges du document Word, en points (1 cm = 28,35 pt) : 1276 / 1274 / 1418 / 1135 twips. */
    public static final float MARGE_GAUCHE = 63.8f;
    public static final float MARGE_DROITE = 63.7f;
    public static final float HAUT_CONTENU = 70.9f;
    public static final float BAS_CONTENU = 56.7f;

    /** Largeur du logo ; sa hauteur suit les proportions du fichier, sans l'étirement de 22 % du document Word. */
    private static final float LOGO_LARGEUR = 150f;
    /** Distance du logo au bord haut de la page (en-tête à 284 twips). */
    private static final float LOGO_DISTANCE_HAUT = 14.2f;

    private static final float PIED_TAILLE = 8f;
    /** Ligne de base du pied de page, au-dessus du bord bas (pied à 396 twips). */
    private static final float PIED_Y = 24f;
    private static final float ICONE_HAUTEUR = 10f;

    private static final String ADRESSE = "Immeuble Adja Koone - Boulevard de Saint Louis, Point E, Dakar – Sénégal";
    private static final String TELEPHONE = "+221 33 865 91 48 / 49";
    private static final String SITE = "www.inpg.sn";
    private static final String SITE_URL = "https://www.inpg.sn";

    private static final Color LIEN = new Color(0x05, 0x63, 0xC1);
    private static final Color GRIS = new Color(70, 70, 78);

    private static final class Images {
        static final byte[] LOGO = PolicesPdf.lire("pdf/logo-inpg.png");
        static final byte[] ICONE_ADRESSE = PolicesPdf.lire("pdf/icone-adresse.png");
        static final byte[] ICONE_TELEPHONE = PolicesPdf.lire("pdf/icone-telephone.png");
    }

    private PapierEnTete() {
    }

    /** Applique le papier à en-tête à toutes les pages du document. */
    public static void apposer(PDDocument document) throws IOException {
        apposer(document, new PolicesPdf(document));
    }

    public static void apposer(PDDocument document, PolicesPdf polices) throws IOException {
        PDImageXObject logo = PDImageXObject.createFromByteArray(document, Images.LOGO, "logo-inpg");
        PDImageXObject iconeAdresse = PDImageXObject.createFromByteArray(document, Images.ICONE_ADRESSE, "icone-adresse");
        PDImageXObject iconeTelephone = PDImageXObject.createFromByteArray(document, Images.ICONE_TELEPHONE, "icone-telephone");

        float logoHauteur = LOGO_LARGEUR * logo.getHeight() / logo.getWidth();

        int total = document.getNumberOfPages();
        for (int numero = 1; numero <= total; numero++) {
            PDPage page = document.getPage(numero - 1);
            PDRectangle format = page.getMediaBox();
            try (PDPageContentStream flux = new PDPageContentStream(document, page, AppendMode.APPEND, true, true)) {
                flux.drawImage(logo, MARGE_GAUCHE, format.getHeight() - LOGO_DISTANCE_HAUT - logoHauteur,
                        LOGO_LARGEUR, logoHauteur);
                piedDePage(page, flux, format, polices, iconeAdresse, iconeTelephone);
                pagination(flux, format, polices, numero, total);
            }
        }
    }

    /**
     * Adresse, téléphone et site sur une ligne centrée, comme dans le modèle ; si la ligne est plus large que la
     * page, l'adresse passe seule sur la première ligne.
     */
    private static void piedDePage(PDPage page, PDPageContentStream flux, PDRectangle format, PolicesPdf polices,
            PDImageXObject iconeAdresse, PDImageXObject iconeTelephone) throws IOException {
        PDFont gras = polices.gras;
        String adresse = PolicesPdf.sur(gras, ADRESSE);
        String telephone = PolicesPdf.sur(gras, TELEPHONE);

        float lAdresse = ICONE_HAUTEUR * iconeAdresse.getWidth() / iconeAdresse.getHeight() + 3 + largeur(gras, adresse);
        float lTelephone = ICONE_HAUTEUR * iconeTelephone.getWidth() / iconeTelephone.getHeight() + 3
                + largeur(gras, telephone) + 8 + largeur(gras, SITE);
        float ecart = 10f;
        float utile = format.getWidth() - 2 * 24f;

        if (lAdresse + ecart + lTelephone <= utile) {
            float x = (format.getWidth() - (lAdresse + ecart + lTelephone)) / 2;
            float suite = ligneAdresse(flux, gras, iconeAdresse, adresse, x, PIED_Y);
            ligneContact(page, flux, gras, iconeTelephone, telephone, suite + ecart, PIED_Y);
        } else {
            ligneAdresse(flux, gras, iconeAdresse, adresse, (format.getWidth() - lAdresse) / 2, PIED_Y + 11);
            ligneContact(page, flux, gras, iconeTelephone, telephone, (format.getWidth() - lTelephone) / 2, PIED_Y);
        }
    }

    /** @return l'abscisse qui suit la ligne */
    private static float ligneAdresse(PDPageContentStream flux, PDFont gras, PDImageXObject icone, String adresse,
            float x, float y) throws IOException {
        float largeurIcone = ICONE_HAUTEUR * icone.getWidth() / icone.getHeight();
        flux.drawImage(icone, x, y - 2, largeurIcone, ICONE_HAUTEUR);
        texte(flux, gras, PIED_TAILLE, Color.BLACK, adresse, x + largeurIcone + 3, y);
        return x + largeurIcone + 3 + largeur(gras, adresse);
    }

    private static void ligneContact(PDPage page, PDPageContentStream flux, PDFont gras, PDImageXObject icone,
            String telephone, float x, float y) throws IOException {
        float largeurIcone = ICONE_HAUTEUR * icone.getWidth() / icone.getHeight();
        flux.drawImage(icone, x, y - 2, largeurIcone, ICONE_HAUTEUR);
        float xTexte = x + largeurIcone + 3;
        texte(flux, gras, PIED_TAILLE, Color.BLACK, telephone, xTexte, y);

        float xSite = xTexte + largeur(gras, telephone) + 8;
        float largeurSite = largeur(gras, SITE);
        texte(flux, gras, PIED_TAILLE, LIEN, SITE, xSite, y);
        flux.setStrokingColor(LIEN);
        flux.setLineWidth(0.6f);
        flux.moveTo(xSite, y - 1.5f);
        flux.lineTo(xSite + largeurSite, y - 1.5f);
        flux.stroke();
        lien(page, xSite, y - 2, largeurSite, PIED_TAILLE + 2);
    }

    private static void pagination(PDPageContentStream flux, PDRectangle format, PolicesPdf polices, int numero, int total)
            throws IOException {
        String libelle = "Page " + numero + " / " + total;
        texte(flux, polices.regulier, 8f, GRIS, libelle,
                format.getWidth() - MARGE_DROITE - largeurEn(polices.regulier, 8f, libelle), 40f);
    }

    private static void lien(PDPage page, float x, float y, float largeur, float hauteur) throws IOException {
        PDAnnotationLink lien = new PDAnnotationLink();
        lien.setRectangle(new PDRectangle(x, y, largeur, hauteur));
        PDBorderStyleDictionary bordure = new PDBorderStyleDictionary();
        bordure.setWidth(0);
        lien.setBorderStyle(bordure);
        PDActionURI action = new PDActionURI();
        action.setURI(SITE_URL);
        lien.setAction(action);
        page.getAnnotations().add(lien);
    }

    private static void texte(PDPageContentStream flux, PDFont police, float taille, Color couleur, String texte, float x,
            float y) throws IOException {
        flux.beginText();
        flux.setNonStrokingColor(couleur);
        flux.setFont(police, taille);
        flux.newLineAtOffset(x, y);
        flux.showText(texte);
        flux.endText();
    }

    /** Largeur d'un texte en points au corps du pied de page (8 pt). */
    private static float largeur(PDFont police, String texte) throws IOException {
        return largeurEn(police, PIED_TAILLE, texte);
    }

    private static float largeurEn(PDFont police, float taille, String texte) throws IOException {
        return police.getStringWidth(texte) / 1000 * taille;
    }
}
