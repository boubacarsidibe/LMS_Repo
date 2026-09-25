package inpg.sn.lms.espace_etudiant.service;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.espace_etudiant.entity.Certificat;

/**
 * Génère le PDF du certificat à la volée (pas de fichier stocké), au modèle officiel de l'INPG.
 *
 * <p>Le cadre, le logo, le filigrane et les textes fixes ne sont pas redessinés : ils viennent du
 * modèle d'origine, dont les zones variables ont été vidées ({@code certificats/certificat-formation-fond.pdf},
 * voir le README du même dossier). Ce service n'y superpose que ce qui change d'un certificat à
 * l'autre — civilité et nom, date de naissance, formation, période, date de délivrance et code — aux
 * positions et dans les polices relevées sur le modèle, plus un petit QR code de vérification.
 *
 * <p>Le QR code n'est pas dans le modèle : il reste parce que la garantie d'authenticité réelle est la
 * signature électronique ({@code CertificatSignatureService}), vérifiable publiquement par ce lien.
 */
@Service
public class CertificatPdfService {

    private static final String FOND = "certificats/certificat-formation-fond.pdf";
    private static final String ARIAL_GRAS = "certificats/polices/LiberationSans-Bold.ttf";
    private static final String ARIAL = "certificats/polices/LiberationSans-Regular.ttf";
    private static final String MONTSERRAT_REGULIER = "certificats/polices/Montserrat-Regular.ttf";
    private static final String MONTSERRAT_CODE = "certificats/polices/Montserrat-SemiBoldItalic.ttf";

    /** Bleu nuit du texte du modèle (0,162 / 0,156 / 0,361). */
    private static final Color BLEU_NUIT = new Color(41, 40, 92);

    /** Centre horizontal de la page du modèle (870,236 pt de large). */
    private static final float CENTRE = 435.118f;
    /** Largeur au-delà de laquelle un nom ou un intitulé est réduit pour rester dans le cadre. */
    private static final float LARGEUR_MAX = 720f;

    private static final float Y_NOM = 359.668f;
    private static final float Y_FORMATION = 280.632f;
    private static final float Y_PERIODE = 239.878f;
    private static final float Y_FAIT_A = 82.368f;
    private static final float X_FAIT_A = 75.401f;
    private static final float Y_CODE = 41.528f;
    private static final float X_CODE = 54.185f;

    /** Écart entre le nom (22 pt gras) et « né le » : une espace de 26 pt dans le modèle. */
    private static final float ECART_NAISSANCE = 7.228f;
    /** Le modèle étire l'intitulé à 81,4 % (20 pt de large pour 24,576 pt de haut). */
    private static final float ECHELLE_FORMATION = 81.38f;
    /** Le code de bas de page est étiré à 115,6 % (9,835 pt de large pour 8,507 pt de haut). */
    private static final float ECHELLE_CODE = 115.61f;

    private static final float QR_TAILLE = 50f;
    private static final float QR_Y = 62f;

    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd", Locale.FRENCH);
    private static final DateTimeFormatter MOIS = DateTimeFormatter.ofPattern("MMMM", Locale.FRENCH);
    private static final DateTimeFormatter JOUR_MOIS_ANNEE = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH);
    private static final DateTimeFormatter DATE_NAISSANCE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final String frontendUrl;
    private final byte[] fond;
    private final byte[] arialGras;
    private final byte[] arial;
    private final byte[] montserratRegulier;
    private final byte[] montserratCode;

    public CertificatPdfService(@Value("${app.frontend-url}") String frontendUrl) {
        this.frontendUrl = frontendUrl;
        this.fond = lire(FOND);
        this.arialGras = lire(ARIAL_GRAS);
        this.arial = lire(ARIAL);
        this.montserratRegulier = lire(MONTSERRAT_REGULIER);
        this.montserratCode = lire(MONTSERRAT_CODE);
    }

    public byte[] genererPdf(Certificat certificat) {
        try (PDDocument document = Loader.loadPDF(fond)) {
            PDPage page = document.getPage(0);
            PDFont arialGras = charger(document, this.arialGras);
            PDFont arial = charger(document, this.arial);
            PDFont montserrat = charger(document, montserratRegulier);
            PDFont montserratCode = charger(document, this.montserratCode);

            try (PDPageContentStream content = new PDPageContentStream(document, page, AppendMode.APPEND, true, true)) {
                content.setNonStrokingColor(BLEU_NUIT);
                ecrireBeneficiaire(content, certificat, arialGras, arial);
                ecrireFormation(content, certificat.getIntituleFormation(), arialGras);

                String periode = libellePeriode(certificat.getDateDebut(), certificat.getDateFin());
                if (periode != null) {
                    ecrireCentre(content, montserrat, 15f, sur(montserrat, periode), Y_PERIODE);
                }

                String fait = "Fait à Dakar, le " + libelleJour(certificat.getDateEmission());
                ecrireA(content, montserrat, 9f, sur(montserrat, fait), X_FAIT_A, Y_FAIT_A, 100f);

                String code = codeAffiche(certificat);
                ecrireA(content, montserratCode, 8.507f, sur(montserratCode, "INPG - Attestation - RDC - " + code),
                        X_CODE, Y_CODE, ECHELLE_CODE);

                dessinerQrVerification(document, content, montserrat, certificat.getReference());
            }

            document.getDocumentInformation().setTitle("Certificat de formation INPG - " + certificat.getReference());
            ByteArrayOutputStream sortie = new ByteArrayOutputStream();
            document.save(sortie);
            return sortie.toByteArray();
        } catch (Exception exception) {
            throw new RuntimeException("La generation du certificat PDF a echoue.", exception);
        }
    }

    /**
     * « Monsieur Cherif Tidiane AIDARA né le 24/08/1985 » : le nom en Arial gras 22 pt, la naissance en
     * Arial 15 pt à sa suite, l'ensemble centré sur la ligne comme dans le modèle. Un nom trop long est
     * réduit plutôt que de sortir du cadre.
     */
    private void ecrireBeneficiaire(PDPageContentStream content, Certificat certificat, PDFont gras,
            PDFont normal) throws IOException {
        String nom = sur(gras, nomAffiche(certificat));
        String naissance = certificat.getDateNaissance() == null ? null
                : sur(normal, libelleNaissance(certificat) + certificat.getDateNaissance().format(DATE_NAISSANCE));

        float taille = 22f;
        float tailleNaissance = 15f;
        float largeur = largeur(gras, nom, taille)
                + (naissance == null ? 0 : ECART_NAISSANCE + largeur(normal, naissance, tailleNaissance));
        if (largeur > LARGEUR_MAX) {
            float facteur = LARGEUR_MAX / largeur;
            taille *= facteur;
            tailleNaissance *= facteur;
            largeur = LARGEUR_MAX;
        }

        float x = CENTRE - largeur / 2;
        ecrireA(content, gras, taille, nom, x, Y_NOM, 100f);
        if (naissance != null) {
            float xNaissance = x + largeur(gras, nom, taille) + ECART_NAISSANCE * taille / 22f;
            ecrireA(content, normal, tailleNaissance, naissance, xNaissance, Y_NOM, 100f);
        }
    }

    private void ecrireFormation(PDPageContentStream content, String intitule, PDFont gras) throws IOException {
        String texte = sur(gras, intitule);
        float taille = 24.576f;
        float echelle = ECHELLE_FORMATION;
        float largeur = largeur(gras, texte, taille) * echelle / 100f;
        if (largeur > LARGEUR_MAX) {
            echelle = echelle * LARGEUR_MAX / largeur;
            largeur = LARGEUR_MAX;
        }
        ecrireA(content, gras, taille, texte, CENTRE - largeur / 2, Y_FORMATION, echelle);
    }

    /** Ajoute le QR de vérification, discret, au centre du bas de page (zone vide du modèle). */
    private void dessinerQrVerification(PDDocument document, PDPageContentStream content, PDFont police,
            String reference) throws Exception {
        byte[] png = genererQrCode(frontendUrl + "/verification-certificat/" + reference);
        PDImageXObject image = PDImageXObject.createFromByteArray(document, png, "qr");
        content.drawImage(image, CENTRE - QR_TAILLE / 2, QR_Y, QR_TAILLE, QR_TAILLE);
        ecrireCentre(content, police, 5.5f, sur(police, "Vérifier l'authenticité"), QR_Y + QR_TAILLE + 4f);
    }

    // ===================== Contenu =====================

    static String nomAffiche(Certificat certificat) {
        Etudiant etudiant = certificat.getEtudiant();
        String prenom = etudiant.getPrenom() == null ? "" : etudiant.getPrenom().trim();
        String nom = etudiant.getNom() == null ? "" : etudiant.getNom().trim().toUpperCase(Locale.FRENCH);
        String identite = (prenom + " " + nom).trim();
        String civilite = certificat.getCivilite();
        return civilite == null || civilite.isBlank() ? identite : civilite.trim() + " " + identite;
    }

    private static String libelleNaissance(Certificat certificat) {
        String civilite = certificat.getCivilite();
        if (civilite == null || civilite.isBlank()) {
            return "né(e) le ";
        }
        return "Monsieur".equalsIgnoreCase(civilite.trim()) ? "né le " : "née le ";
    }

    /** Le code propre au certificat, à défaut sa référence (certificats délivrés avant le modèle INPG). */
    private static String codeAffiche(Certificat certificat) {
        String code = certificat.getCodeCertificat();
        return code == null || code.isBlank() ? certificat.getReference() : code.trim();
    }

    private static String libelleJour(LocalDate date) {
        return date == null ? "-" : date.format(JOUR_MOIS_ANNEE);
    }

    /**
     * « Du 02 au 06 février 2026 », « Du 28 janvier au 06 février 2026 », « Du 28 décembre 2025 au
     * 06 janvier 2026 » ou « Le 06 février 2026 » ; {@code null} si la période est inconnue
     * (certificat délivré avant l'ajout de ce champ).
     */
    static String libellePeriode(LocalDate debut, LocalDate fin) {
        if (debut == null && fin == null) {
            return null;
        }
        if (debut == null || fin == null || debut.equals(fin)) {
            return "Le " + libelleJour(debut == null ? fin : debut);
        }
        if (debut.getYear() == fin.getYear() && debut.getMonth() == fin.getMonth()) {
            return "Du " + debut.format(JOUR) + " au " + fin.format(JOUR_MOIS_ANNEE);
        }
        if (debut.getYear() == fin.getYear()) {
            return "Du " + debut.format(JOUR) + " " + debut.format(MOIS) + " au " + fin.format(JOUR_MOIS_ANNEE);
        }
        return "Du " + debut.format(JOUR_MOIS_ANNEE) + " au " + fin.format(JOUR_MOIS_ANNEE);
    }

    // ===================== Dessin =====================

    private byte[] genererQrCode(String contenu) throws Exception {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(contenu, BarcodeFormat.QR_CODE, 300, 300);
        BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", sortie);
        return sortie.toByteArray();
    }

    private void ecrireCentre(PDPageContentStream content, PDFont font, float taille, String texte, float y)
            throws IOException {
        ecrireA(content, font, taille, texte, CENTRE - largeur(font, texte, taille) / 2, y, 100f);
    }

    /** @param echelle étirement horizontal en pourcentage (100 = aucun). */
    private void ecrireA(PDPageContentStream content, PDFont font, float taille, String texte, float x, float y,
            float echelle) throws IOException {
        content.beginText();
        content.setFont(font, taille);
        content.setHorizontalScaling(echelle);
        content.newLineAtOffset(x, y);
        content.showText(texte);
        content.endText();
    }

    private static float largeur(PDFont font, String texte, float taille) throws IOException {
        return font.getStringWidth(texte) / 1000 * taille;
    }

    // ===================== Textes sûrs =====================

    /**
     * Un caractère absent de la police (émoji, alphabet non couvert dans un nom saisi librement) devient '?'
     * plutôt que de lever une {@code IllegalArgumentException} en pleine génération.
     */
    private static String sur(PDFont font, String valeur) {
        StringBuilder resultat = new StringBuilder(valeur.length());
        valeur.codePoints().forEach(cp -> {
            String caractere = new String(Character.toChars(cp));
            try {
                font.encode(caractere);
                resultat.append(caractere);
            } catch (IOException | IllegalArgumentException absent) {
                resultat.append('?');
            }
        });
        return resultat.toString();
    }

    private static PDFont charger(PDDocument document, byte[] police) throws IOException {
        return PDType0Font.load(document, new ByteArrayInputStream(police), true);
    }

    private static byte[] lire(String chemin) {
        try {
            return new ClassPathResource(chemin).getInputStream().readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Ressource de certificat introuvable : " + chemin, e);
        }
    }
}
