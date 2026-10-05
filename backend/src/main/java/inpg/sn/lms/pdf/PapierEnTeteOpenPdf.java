package inpg.sn.lms.pdf;

import java.awt.Color;
import java.io.IOException;
import java.util.List;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfAction;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;

/**
 * Le papier à en-tête de l'INPG ({@link PapierEnTete}) pour les documents produits avec OpenPDF : reçu de paiement,
 * lettre d'admission, convocation. Même logo, même pied de page (adresse, téléphone, site cliquable), mêmes marges.
 *
 * <p>Usage : {@code new Document(PageSize.A4, PapierEnTete.MARGE_GAUCHE, PapierEnTete.MARGE_DROITE,
 * PapierEnTete.HAUT_CONTENU, PapierEnTete.BAS_CONTENU)} puis {@code writer.setPageEvent(new PapierEnTeteOpenPdf())}.
 */
public final class PapierEnTeteOpenPdf extends PdfPageEventHelper {

    private static final float LOGO_LARGEUR = 150f;
    private static final float LOGO_DISTANCE_HAUT = 14.2f;
    private static final float PIED_TAILLE = 8f;
    private static final float PIED_Y = 24f;
    private static final float ICONE_HAUTEUR = 10f;

    private static final String ADRESSE = "Immeuble Adja Koone - Boulevard de Saint Louis, Point E, Dakar – Sénégal";
    private static final String TELEPHONE = "+221 33 865 91 48 / 49";
    private static final String SITE = "www.inpg.sn";
    private static final String SITE_URL = "https://www.inpg.sn";
    private static final Color LIEN = new Color(0x05, 0x63, 0xC1);

    private static final class Octets {
        static final byte[] LOGO = PolicesPdf.lire("pdf/logo-inpg.png");
        static final byte[] ICONE_ADRESSE = PolicesPdf.lire("pdf/icone-adresse.png");
        static final byte[] ICONE_TELEPHONE = PolicesPdf.lire("pdf/icone-telephone.png");
    }

    /**
     * Ajoute des paragraphes <strong>un à un</strong> au document. Les imbriquer dans un paragraphe conteneur
     * (l'ancienne façon de « regrouper » un bloc) fait perdre à OpenPDF leur espacement et leur alignement : titres
     * décentrés, blocs qui se recouvrent.
     */
    public static void ajouter(Document document, List<? extends Element> elements) throws DocumentException {
        for (Element element : elements) {
            document.add(element);
        }
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
        try {
            PdfContentByte flux = writer.getDirectContent();
            Rectangle page = document.getPageSize();

            Image logo = Image.getInstance(Octets.LOGO);
            float logoHauteur = LOGO_LARGEUR * logo.getHeight() / logo.getWidth();
            logo.scaleAbsolute(LOGO_LARGEUR, logoHauteur);
            logo.setAbsolutePosition(PapierEnTete.MARGE_GAUCHE, page.getHeight() - LOGO_DISTANCE_HAUT - logoHauteur);
            flux.addImage(logo);

            piedDePage(flux, page);
        } catch (DocumentException | IOException e) {
            throw new IllegalStateException("Le papier à en-tête de l'INPG n'a pas pu être posé.", e);
        }
    }

    private void piedDePage(PdfContentByte flux, Rectangle page) throws DocumentException, IOException {
        BaseFont gras = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
        Image iconeAdresse = Image.getInstance(Octets.ICONE_ADRESSE);
        Image iconeTelephone = Image.getInstance(Octets.ICONE_TELEPHONE);
        float lIconeAdresse = ICONE_HAUTEUR * iconeAdresse.getWidth() / iconeAdresse.getHeight();
        float lIconeTelephone = ICONE_HAUTEUR * iconeTelephone.getWidth() / iconeTelephone.getHeight();

        float lAdresse = lIconeAdresse + 3 + gras.getWidthPoint(ADRESSE, PIED_TAILLE);
        float lSite = gras.getWidthPoint(SITE, PIED_TAILLE);
        float lTelephone = lIconeTelephone + 3 + gras.getWidthPoint(TELEPHONE, PIED_TAILLE) + 8 + lSite;
        float ecart = 10f;

        float xAdresse;
        float xTelephone;
        float yAdresse = PIED_Y;
        if (lAdresse + ecart + lTelephone <= page.getWidth() - 48f) {
            xAdresse = (page.getWidth() - (lAdresse + ecart + lTelephone)) / 2;
            xTelephone = xAdresse + lAdresse + ecart;
        } else {
            yAdresse = PIED_Y + 11;
            xAdresse = (page.getWidth() - lAdresse) / 2;
            xTelephone = (page.getWidth() - lTelephone) / 2;
        }

        icone(flux, iconeAdresse, lIconeAdresse, xAdresse, yAdresse);
        texte(flux, gras, Color.BLACK, ADRESSE, xAdresse + lIconeAdresse + 3, yAdresse);

        icone(flux, iconeTelephone, lIconeTelephone, xTelephone, PIED_Y);
        float xTexte = xTelephone + lIconeTelephone + 3;
        texte(flux, gras, Color.BLACK, TELEPHONE, xTexte, PIED_Y);
        float xSite = xTexte + gras.getWidthPoint(TELEPHONE, PIED_TAILLE) + 8;
        texte(flux, gras, LIEN, SITE, xSite, PIED_Y);
        flux.setColorStroke(LIEN);
        flux.setLineWidth(0.6f);
        flux.moveTo(xSite, PIED_Y - 1.5f);
        flux.lineTo(xSite + lSite, PIED_Y - 1.5f);
        flux.stroke();
        flux.setAction(new PdfAction(SITE_URL), xSite, PIED_Y - 2, xSite + lSite, PIED_Y + PIED_TAILLE);
    }

    private static void icone(PdfContentByte flux, Image icone, float largeur, float x, float y) throws DocumentException {
        icone.scaleAbsolute(largeur, ICONE_HAUTEUR);
        icone.setAbsolutePosition(x, y - 2);
        flux.addImage(icone);
    }

    private static void texte(PdfContentByte flux, BaseFont police, Color couleur, String texte, float x, float y) {
        flux.beginText();
        flux.setColorFill(couleur);
        flux.setFontAndSize(police, PIED_TAILLE);
        flux.setTextMatrix(x, y);
        flux.showText(texte);
        flux.endText();
    }
}
