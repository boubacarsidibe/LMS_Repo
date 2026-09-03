package inpg.sn.lms.espace_etudiant.service;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.format.DateTimeFormatter;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.util.Matrix;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import inpg.sn.lms.espace_etudiant.entity.Certificat;

/**
 * Génère le PDF du certificat à la volée (pas de fichier stocké) : cadre,
 * QR code de vérification, cachet/signature dessinés vectoriellement et
 * filigrane répété pour limiter la contrefaçon.
 *
 * <p>Le cachet dessiné reste cosmétique — la garantie d'authenticité réelle
 * est la signature électronique (HMAC, {@code CertificatSignatureService})
 * que le QR code permet de vérifier publiquement.
 */
@Service
public class CertificatPdfService {

    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    /** Encodage réellement utilisé par Helvetica dans PDFBox. */
    private static final Charset WINANSI = Charset.forName("windows-1252");

    private final String frontendUrl;

    public CertificatPdfService(@Value("${app.frontend-url}") String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }

    public byte[] genererPdf(Certificat certificat) {
        return genererPdf(certificat, null);
    }

    /**
     * @param moyenneGenerale moyenne générale de l'étudiant sur 20, pour porter
     *        une mention sur le document ; {@code null} pour l'omettre.
     */
    public byte[] genererPdf(Certificat certificat, Double moyenneGenerale) {
        try (PDDocument document = new PDDocument()) {
            PDRectangle paysage = new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
            PDPage page = new PDPage(paysage);
            document.addPage(page);

            float largeur = paysage.getWidth();
            float hauteur = paysage.getHeight();

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                PDType1Font titreFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                PDType1Font texteFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                PDType1Font texteItalique = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

                dessinerFiligrane(content, certificat, texteFont);

                content.setStrokingColor(new Color(30, 64, 120));
                content.setLineWidth(3f);
                content.addRect(30, 30, largeur - 60, hauteur - 60);
                content.stroke();
                content.setLineWidth(1f);
                content.addRect(38, 38, largeur - 76, hauteur - 76);
                content.stroke();

                ecrireCentre(content, titreFont, 26, "CERTIFICAT DE FORMATION", largeur, hauteur - 100);
                ecrireCentre(content, texteFont, 13, "Institut National du Pétrole et du Gaz", largeur, hauteur - 130);

                String etudiantNom = String.join(" ", certificat.getEtudiant().getPrenom(), certificat.getEtudiant().getNom());
                ecrireCentre(content, texteFont, 12, "Décerné à", largeur, hauteur - 190);
                ecrireCentre(content, titreFont, 22, etudiantNom, largeur, hauteur - 220);
                if (certificat.getEtudiant().getMatricule() != null) {
                    ecrireCentre(content, texteItalique, 10, "Matricule : " + certificat.getEtudiant().getMatricule(), largeur, hauteur - 240);
                }

                ecrireCentre(content, texteFont, 12, "pour avoir suivi avec succès la formation", largeur, hauteur - 280);
                ecrireCentre(content, titreFont, 16, certificat.getIntituleFormation(), largeur, hauteur - 305);
                if (certificat.getPromotion() != null) {
                    ecrireCentre(content, texteFont, 11, "Promotion : " + certificat.getPromotion().getLibelle(), largeur, hauteur - 328);
                }

                String mention = mention(moyenneGenerale);
                if (mention != null) {
                    ecrireCentre(content, texteFont, 11, "Mention : " + mention, largeur, hauteur - 348);
                }

                String dateEmission = certificat.getDateEmission() == null ? "-"
                        : certificat.getDateEmission().format(FORMAT_DATE);
                ecrireCentre(content, texteFont, 11,
                        "Délivré le " + dateEmission + "  -  Référence : " + certificat.getReference(),
                        largeur, hauteur - (mention != null ? 378 : 365));

                dessinerCachet(content, texteFont, titreFont, 110, 90);
                content.setStrokingColor(Color.BLACK);
                content.setLineWidth(0.8f);
                content.moveTo(200, 100);
                content.lineTo(380, 100);
                content.stroke();
                ecrireCentre(content, texteItalique, 9, "Signature du responsable pédagogique", 580, 85);

                byte[] qrPng = genererQrCode(frontendUrl + "/verification-certificat/" + certificat.getReference());
                PDImageXObject qrImage = PDImageXObject.createFromByteArray(document, qrPng, "qr");
                float qrTaille = 90;
                content.drawImage(qrImage, largeur - qrTaille - 60, 66, qrTaille, qrTaille);
                ecrireCentreA(content, texteItalique, 7,
                        "Vérifier l'authenticité", largeur - qrTaille / 2 - 60, 58);
            }

            ByteArrayOutputStream sortie = new ByteArrayOutputStream();
            document.save(sortie);
            return sortie.toByteArray();
        } catch (Exception exception) {
            throw new RuntimeException("La generation du certificat PDF a echoue.", exception);
        }
    }

    private void dessinerFiligrane(PDPageContentStream content, Certificat certificat, PDType1Font font) throws IOException {
        PDExtendedGraphicsState etatTransparence = new PDExtendedGraphicsState();
        etatTransparence.setNonStrokingAlphaConstant(0.08f);
        content.setGraphicsStateParameters(etatTransparence);
        content.setNonStrokingColor(Color.BLACK);

        String texte = ("INPG - " + certificat.getReference() + " - ").repeat(6);
        content.beginText();
        content.setFont(font, 28);
        for (int ligne = 0; ligne < 6; ligne++) {
            float y = ligne * 130 - 40;
            content.setTextMatrix(Matrix.getRotateInstance(Math.toRadians(30), -50, y));
            content.showText(texte);
        }
        content.endText();

        PDExtendedGraphicsState etatNormal = new PDExtendedGraphicsState();
        etatNormal.setNonStrokingAlphaConstant(1f);
        content.setGraphicsStateParameters(etatNormal);
    }

    private void dessinerCachet(PDPageContentStream content, PDType1Font font, PDType1Font fontGras, float x, float y) throws IOException {
        content.setStrokingColor(new Color(180, 30, 30));
        content.setLineWidth(2f);
        dessinerCercle(content, x, y, 55);
        content.stroke();
        content.setNonStrokingColor(new Color(180, 30, 30));
        ecrireCentreA(content, fontGras, 10, "INPG", x, y + 8);
        ecrireCentreA(content, font, 8, "CERTIFIÉ", x, y - 8);
        content.setNonStrokingColor(Color.BLACK);
    }

    /** addEllipse n'est pas disponible sur cette version de PDFBox : approximation par segments. */
    private void dessinerCercle(PDPageContentStream content, float centreX, float centreY, float rayon) throws IOException {
        int segments = 48;
        for (int i = 0; i <= segments; i++) {
            double angle = 2 * Math.PI * i / segments;
            float px = centreX + (float) (rayon * Math.cos(angle));
            float py = centreY + (float) (rayon * Math.sin(angle));
            if (i == 0) {
                content.moveTo(px, py);
            } else {
                content.lineTo(px, py);
            }
        }
    }

    private byte[] genererQrCode(String contenu) throws Exception {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(contenu, BarcodeFormat.QR_CODE, 300, 300);
        BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", sortie);
        return sortie.toByteArray();
    }

    private void ecrireCentre(PDPageContentStream content, PDType1Font font, float taille, String texte, float largeurPage, float y) throws IOException {
        String sur = texteSur(texte);
        float largeurTexte = font.getStringWidth(sur) / 1000 * taille;
        float x = (largeurPage - largeurTexte) / 2;
        ecrireTexte(content, font, taille, sur, x, y);
    }

    private void ecrireCentreA(PDPageContentStream content, PDType1Font font, float taille, String texte, float centreX, float y) throws IOException {
        String sur = texteSur(texte);
        float largeurTexte = font.getStringWidth(sur) / 1000 * taille;
        ecrireTexte(content, font, taille, sur, centreX - largeurTexte / 2, y);
    }

    private void ecrireTexte(PDPageContentStream content, PDType1Font font, float taille, String texte, float x, float y) throws IOException {
        content.beginText();
        content.setFont(font, taille);
        content.newLineAtOffset(x, y);
        content.showText(texteSur(texte));
        content.endText();
    }

    /** Mention académique à partir de la moyenne /20, ou {@code null} si non disponible. */
    private String mention(Double moyenneSur20) {
        if (moyenneSur20 == null) {
            return null;
        }
        if (moyenneSur20 >= 16) {
            return "Très bien";
        }
        if (moyenneSur20 >= 14) {
            return "Bien";
        }
        if (moyenneSur20 >= 12) {
            return "Assez bien";
        }
        return "Passable";
    }

    /**
     * Rend un texte sûr pour l'encodage WinAnsi de Helvetica : les accents
     * latins passent tels quels, seul un caractère hors Latin-1 (émoji, alphabet
     * non latin dans un nom saisi librement) est remplacé par '?', ce qui évite
     * un {@code IllegalArgumentException} en pleine génération.
     */
    private static String texteSur(String valeur) {
        if (valeur == null) {
            return "";
        }
        StringBuilder resultat = new StringBuilder(valeur.length());
        var encodeur = WINANSI.newEncoder();
        for (char c : valeur.toCharArray()) {
            resultat.append(encodeur.canEncode(c) ? c : '?');
        }
        return resultat.toString();
    }
}
