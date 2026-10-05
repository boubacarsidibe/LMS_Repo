package inpg.sn.lms.pdf;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.core.io.ClassPathResource;

/**
 * Polices des documents PDF de l'INPG : Liberation Sans (métriquement compatible avec Arial), embarquée.
 *
 * <p>Les anciens rapports utilisaient Helvetica non embarquée, limitée à Latin-1 : l'apparence dépendait du lecteur
 * de PDF et tout caractère hors Latin-1 devenait un « ? ». Embarquée, la police est identique partout et couvre
 * l'écriture latine étendue, le grec et le cyrillique.
 */
public final class PolicesPdf {

    private static final class Octets {
        static final byte[] REGULIER = lire("pdf/polices/LiberationSans-Regular.ttf");
        static final byte[] GRAS = lire("pdf/polices/LiberationSans-Bold.ttf");
        static final byte[] ITALIQUE = lire("pdf/polices/LiberationSans-Italic.ttf");
    }

    public final PDFont regulier;
    public final PDFont gras;
    public final PDFont italique;

    public PolicesPdf(PDDocument document) throws IOException {
        this.regulier = PDType0Font.load(document, new ByteArrayInputStream(Octets.REGULIER), true);
        this.gras = PDType0Font.load(document, new ByteArrayInputStream(Octets.GRAS), true);
        this.italique = PDType0Font.load(document, new ByteArrayInputStream(Octets.ITALIQUE), true);
    }

    /**
     * Un caractère absent de la police (émoji, alphabet non couvert glissé dans un titre saisi librement) devient
     * '?' plutôt que de lever une {@code IllegalArgumentException} en pleine génération ; les retours à la ligne et
     * tabulations deviennent des espaces.
     */
    public static String sur(PDFont police, String valeur) {
        if (valeur == null) {
            return "";
        }
        StringBuilder resultat = new StringBuilder(valeur.length());
        valeur.codePoints().forEach(cp -> {
            if (cp == '\n' || cp == '\r' || cp == '\t') {
                resultat.append(' ');
                return;
            }
            String caractere = new String(Character.toChars(cp));
            try {
                police.encode(caractere);
                resultat.append(caractere);
            } catch (IOException | IllegalArgumentException absent) {
                resultat.append('?');
            }
        });
        return resultat.toString();
    }

    static byte[] lire(String chemin) {
        try {
            return new ClassPathResource(chemin).getInputStream().readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Ressource PDF introuvable : " + chemin, e);
        }
    }
}
