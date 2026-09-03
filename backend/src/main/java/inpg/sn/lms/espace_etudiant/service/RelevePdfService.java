package inpg.sn.lms.espace_etudiant.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.format.DateTimeFormatter;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.util.Matrix;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.espace_etudiant.entity.Formation;
import inpg.sn.lms.espace_etudiant.entity.Promotion;
import inpg.sn.lms.espace_etudiant.entity.ReleveLigne;
import inpg.sn.lms.espace_etudiant.entity.ReleveNotes;

/**
 * Génère le PDF d'un relevé de notes <strong>délivré</strong> (TDR §3.x).
 *
 * <p>Met en page les valeurs <strong>figées</strong> de {@link ReleveNotes} et
 * {@link ReleveLigne} : code, titre, coefficient et moyenne y sont recopiés à la
 * délivrance (migration V9), jamais recalculés. Deux relevés d'un même étudiant à
 * deux dates peuvent donc différer — c'est la définition d'un document.
 *
 * <p>Même bibliothèque (PDFBox) et même esprit que {@code CertificatPdfService},
 * en A4 portrait : en-tête institutionnel, identité, tableau des cours, moyenne
 * générale pondérée et mention, filigrane léger anti-contrefaçon.
 */
@Service
public class RelevePdfService {

    private static final float MARGE = 50f;
    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Color BLEU_NUIT = new Color(19, 23, 65);
    /** Encodage réellement utilisé par Helvetica dans PDFBox. */
    private static final Charset WINANSI = Charset.forName("windows-1252");

    /** Génère le PDF et l'emballe en réponse HTTP « pièce jointe » au nom {@code INPG-releve-<référence>.pdf}. */
    public ResponseEntity<byte[]> reponsePdf(ReleveNotes releve) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("INPG-releve-" + releve.getReference() + ".pdf").build().toString())
                .body(genererPdf(releve));
    }

    public byte[] genererPdf(ReleveNotes releve) {
        try (PDDocument document = new PDDocument()) {
            Contexte ctx = new Contexte(document);
            filigrane(ctx, releve.getReference());

            ctx.titre("RELEVÉ DE NOTES");
            ctx.texte(ctx.normal, 11, "Institut National du Pétrole et du Gaz");
            ctx.texte(ctx.italique, 9,
                    "Référence : " + releve.getReference()
                            + "   -   Émis le " + (releve.getDateEmission() == null ? "-"
                                    : releve.getDateEmission().format(FORMAT_DATE))
                            + "   -   " + nn(releve.getDelivrePar()));
            ctx.espace(14);

            Etudiant etudiant = releve.getEtudiant();
            Promotion promotion = releve.getPromotion() != null ? releve.getPromotion()
                    : (etudiant != null ? etudiant.getPromotion() : null);
            Formation formation = promotion != null ? promotion.getFormation() : null;

            ctx.sousTitre("Étudiant");
            if (etudiant != null) {
                ctx.ligneKpi("Nom et prénom", nn(etudiant.getPrenom()) + " " + nn(etudiant.getNom()));
                ctx.ligneKpi("Matricule", etudiant.getMatricule() == null ? "-" : etudiant.getMatricule());
            }
            ctx.ligneKpi("Promotion", promotion == null ? "-" : promotion.getLibelle());
            ctx.ligneKpi("Formation", formation == null ? "-" : formation.getIntitule());
            ctx.ligneKpi("Période couverte", nn(releve.getPeriode()));
            ctx.espace(14);

            ctx.sousTitre("Résultats par cours");
            enteteTableau(ctx);
            if (releve.getLignes().isEmpty()) {
                ctx.texte(ctx.italique, 10, "Aucune note figée sur ce relevé.");
            } else {
                for (ReleveLigne ligne : releve.getLignes()) {
                    ligneTableau(ctx,
                            nn(ligne.getCoursCode()),
                            nn(ligne.getCoursTitre()),
                            nombre(ligne.getCoefficient()),
                            ligne.getMoyenne() == null ? "-" : nombre(ligne.getMoyenne()) + " / 20");
                }
            }
            ctx.espace(14);

            ctx.sousTitre("Synthèse");
            String moyenne = releve.getMoyenneGenerale() == null ? "-"
                    : nombre(releve.getMoyenneGenerale()) + " / 20";
            ctx.ligneKpi("Moyenne générale (pondérée par les coefficients)", moyenne);
            ctx.ligneKpi("Nombre de notes prises en compte", String.valueOf(releve.getNombreNotes()));
            String mention = mention(releve.getMoyenneGenerale());
            if (mention != null) {
                ctx.ligneKpi("Mention", mention);
            }
            ctx.espace(16);
            ctx.texte(ctx.italique, 9,
                    "Document figé à sa date d'émission : ses valeurs ne sont pas recalculées.");

            ctx.fermer();
            ByteArrayOutputStream sortie = new ByteArrayOutputStream();
            document.save(sortie);
            return sortie.toByteArray();
        } catch (Exception exception) {
            throw new RuntimeException("La génération du relevé PDF a échoué.", exception);
        }
    }

    private void filigrane(Contexte ctx, String reference) throws IOException {
        PDExtendedGraphicsState transparent = new PDExtendedGraphicsState();
        transparent.setNonStrokingAlphaConstant(0.06f);
        ctx.content.setGraphicsStateParameters(transparent);
        ctx.content.setNonStrokingColor(Color.BLACK);
        ctx.content.beginText();
        ctx.content.setFont(ctx.gras, 26);
        String texte = ("INPG - " + reference + "   ").repeat(4);
        for (int i = 0; i < 7; i++) {
            ctx.content.setTextMatrix(Matrix.getRotateInstance(Math.toRadians(30), -60, i * 120 - 30));
            ctx.content.showText(texteSur(texte));
        }
        ctx.content.endText();
        PDExtendedGraphicsState opaque = new PDExtendedGraphicsState();
        opaque.setNonStrokingAlphaConstant(1f);
        ctx.content.setGraphicsStateParameters(opaque);
    }

    private void enteteTableau(Contexte ctx) throws IOException {
        ctx.colonnes("Code", "Intitulé du cours", "Coef.", "Moyenne", ctx.gras);
    }

    private void ligneTableau(Contexte ctx, String code, String titre, String coef, String moyenne) throws IOException {
        ctx.colonnes(code, titre, coef, moyenne, ctx.normal);
    }

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
        return moyenneSur20 >= 10 ? "Passable" : "Insuffisant";
    }

    private static String nombre(double valeur) {
        return String.valueOf(Math.round(valeur * 100) / 100.0);
    }

    private static String nn(String valeur) {
        return valeur == null ? "-" : valeur;
    }

    /** Rend un texte sûr pour l'encodage WinAnsi de Helvetica (voir CertificatPdfService). */
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

    /** Machine à écrire verticale avec saut de page automatique + rendu 4 colonnes. */
    private static final class Contexte {
        private final PDDocument document;
        private PDPage page;
        private PDPageContentStream content;
        private float y;
        private final float largeurUtile;

        final PDType1Font normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        final PDType1Font gras = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        final PDType1Font italique = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

        // Positions de colonnes (x, en points depuis la marge gauche).
        private static final float COL_CODE = 0f;
        private static final float COL_TITRE = 90f;
        private static final float COL_COEF = 360f;
        private static final float COL_MOY = 420f;

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

        private void assurerEspace(float hauteur) throws IOException {
            if (y - hauteur < MARGE) {
                nouvellePage();
            }
        }

        void titre(String texte) throws IOException {
            assurerEspace(30);
            content.setNonStrokingColor(BLEU_NUIT);
            ecrire(gras, 22, texte, 0f);
            content.setNonStrokingColor(Color.BLACK);
            y -= 6;
        }

        void sousTitre(String texte) throws IOException {
            espace(6);
            assurerEspace(22);
            content.setStrokingColor(BLEU_NUIT);
            content.setLineWidth(1f);
            content.moveTo(MARGE, y + 4);
            content.lineTo(MARGE + largeurUtile, y + 4);
            content.stroke();
            ecrire(gras, 13, texte, 0f);
        }

        void ligneKpi(String libelle, String valeur) throws IOException {
            assurerEspace(16);
            ecrire(normal, 11, libelle + " : " + valeur, 0f);
        }

        void texte(PDType1Font font, float taille, String texte) throws IOException {
            assurerEspace(16);
            ecrire(font, taille, texte, 0f);
        }

        void colonnes(String code, String titre, String coef, String moyenne, PDType1Font font) throws IOException {
            assurerEspace(16);
            content.beginText();
            content.setFont(font, 10);
            content.newLineAtOffset(MARGE + COL_CODE, y);
            content.showText(texteSur(tronquer(code, font, 10, COL_TITRE - COL_CODE - 4)));
            content.newLineAtOffset(COL_TITRE - COL_CODE, 0);
            content.showText(texteSur(tronquer(titre, font, 10, COL_COEF - COL_TITRE - 6)));
            content.newLineAtOffset(COL_COEF - COL_TITRE, 0);
            content.showText(texteSur(coef));
            content.newLineAtOffset(COL_MOY - COL_COEF, 0);
            content.showText(texteSur(moyenne));
            content.endText();
            y -= 16;
        }

        void espace(float hauteur) {
            y -= hauteur;
        }

        private void ecrire(PDType1Font font, float taille, String texte, float retrait) throws IOException {
            content.beginText();
            content.setFont(font, taille);
            content.newLineAtOffset(MARGE + retrait, y);
            content.showText(texteSur(tronquer(texte, font, taille, largeurUtile - retrait)));
            content.endText();
            y -= taille + 6;
        }

        private String tronquer(String texte, PDType1Font font, float taille, float largeurMax) throws IOException {
            String sur = texteSur(texte);
            if (font.getStringWidth(sur) / 1000 * taille <= largeurMax) {
                return sur;
            }
            StringBuilder resultat = new StringBuilder();
            for (char c : sur.toCharArray()) {
                if (font.getStringWidth(resultat.toString() + c + "...") / 1000 * taille > largeurMax) {
                    break;
                }
                resultat.append(c);
            }
            return resultat + "...";
        }

        void fermer() throws IOException {
            content.close();
        }
    }
}
