package inpg.sn.lms.espace_etudiant.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import inpg.sn.lms.espace_etudiant.dto.RapportFormateursDto;

/**
 * Rapport des comptes formateurs, en PDF (demande responsable pédagogique) :
 * effectif complet avec coordonnées et cours affectés, en signalant
 * distinctement les formateurs sans aucun cours. Même patron que
 * {@code RapportRecrutementPdfService} (recrutement) — dupliqué plutôt que
 * partagé, comme les autres rapports PDF de ce dépôt.
 *
 * <p>Aucun chiffre n'est recalculé ici : le service met seulement en page le
 * {@link RapportFormateursDto} déjà produit par
 * {@code PedagogieController#construireRapportFormateurs}.
 */
@Service
public class RapportFormateursPdfService {

    private static final float MARGE = 50f;
    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMAT_DATE_HEURE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Charset WINANSI = Charset.forName("windows-1252");

    private static final Color NAVY = new Color(19, 23, 65);
    private static final Color GRIS_TEXTE = new Color(70, 70, 78);
    private static final Color GRIS_BORDURE = new Color(224, 226, 229);
    private static final Color GRIS_FOND_TUILE = new Color(247, 248, 250);
    private static final Color ROUGE_ALERTE = new Color(179, 38, 30);

    public byte[] genererRapport(RapportFormateursDto rapport) {
        String genereLe = "Généré le " + LocalDateTime.now().format(FORMAT_DATE_HEURE);
        try (PDDocument document = new PDDocument()) {
            Contexte ctx = new Contexte(document, "Rapport des comptes formateurs", genereLe);

            ctx.statTuiles(new String[][] {
                    { String.valueOf(rapport.totalFormateurs()), "Formateurs" },
                    { String.valueOf(rapport.formateursSansCours()), "Sans cours affecté" },
            });
            ctx.espace(20);

            ctx.sousTitre("Effectif");
            if (rapport.formateurs().isEmpty()) {
                ctx.texteSimple(ctx.italique, 10, GRIS_TEXTE, "Aucun compte formateur pour le moment.");
            }
            for (RapportFormateursDto.FormateurRapport f : rapport.formateurs()) {
                ctx.ligneListe((nvl(f.prenom()) + " " + nvl(f.nom())).trim()
                        + (f.identifiant() != null ? " (" + f.identifiant() + ")" : ""));
                String detail = nvl(f.email())
                        + (f.telephone() != null ? "  |  " + f.telephone() : "")
                        + (f.dateCreationCompte() != null ? "  |  Compte créé le " + f.dateCreationCompte().format(FORMAT_DATE) : "");
                ctx.ligneDetail(detail);
                if (f.coursEnseignes().isEmpty()) {
                    ctx.ligneDetailAlerte("Aucun cours affecté — invisible dans son propre espace formateur.");
                } else {
                    ctx.ligneDetail("Cours : " + String.join(", ", f.coursEnseignes()));
                }
            }

            ctx.fermerAvecPiedDePage();
            return octets(document);
        } catch (Exception exception) {
            throw new RuntimeException("La génération du rapport des comptes formateurs a échoué.", exception);
        }
    }

    private String nvl(String valeur) {
        return valeur == null ? "" : valeur;
    }

    private byte[] octets(PDDocument document) throws IOException {
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        document.save(sortie);
        return sortie.toByteArray();
    }

    private static String texteSur(String valeur) {
        if (valeur == null) {
            return "";
        }
        StringBuilder resultat = new StringBuilder(valeur.length());
        var encodeur = WINANSI.newEncoder();
        for (char c : valeur.toCharArray()) {
            resultat.append(c == '\n' || c == '\r' || c == '\t' ? ' '
                    : (encodeur.canEncode(c) ? c : '?'));
        }
        return resultat.toString();
    }

    /** Machine à écrire verticale avec bandeau de marque, saut de page automatique et pied de page paginé. */
    private static final class Contexte {
        private static final float HAUTEUR_BANDEAU = 62f;
        private static final float HAUTEUR_PIED = 28f;

        private final PDDocument document;
        private final String titreRapport;
        private final String genereLe;
        private PDPage page;
        private PDPageContentStream content;
        private float y;
        private final float largeurUtile;
        private final float largeurPage = PDRectangle.A4.getWidth();
        private final float hauteurPage = PDRectangle.A4.getHeight();

        final PDType1Font normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        final PDType1Font gras = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        final PDType1Font italique = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

        Contexte(PDDocument document, String titreRapport, String genereLe) throws IOException {
            this.document = document;
            this.titreRapport = titreRapport;
            this.genereLe = genereLe;
            this.largeurUtile = PDRectangle.A4.getWidth() - 2 * MARGE;
            nouvellePage();
        }

        private void nouvellePage() throws IOException {
            if (content != null) {
                content.close();
            }
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            content = new PDPageContentStream(document, page);
            dessinerBandeau();
            y = hauteurPage - HAUTEUR_BANDEAU - 30f;
        }

        private void dessinerBandeau() throws IOException {
            content.setNonStrokingColor(NAVY);
            content.addRect(0, hauteurPage - HAUTEUR_BANDEAU, largeurPage, HAUTEUR_BANDEAU);
            content.fill();

            texteA(MARGE, hauteurPage - 24, gras, 15, Color.WHITE, "INPG");
            texteA(MARGE, hauteurPage - 40, normal, 9, Color.WHITE, "Institut National du Pétrole et du Gaz");

            float largeurTitre = largeurTexte(gras, 13, titreRapport);
            texteA(largeurPage - MARGE - largeurTitre, hauteurPage - 24, gras, 13, Color.WHITE, titreRapport);
            float largeurDate = largeurTexte(normal, 9, genereLe);
            texteA(largeurPage - MARGE - largeurDate, hauteurPage - 40, normal, 9, Color.WHITE, genereLe);

            content.setNonStrokingColor(Color.BLACK);
        }

        private void assurerEspace(float hauteurNecessaire) throws IOException {
            if (y - hauteurNecessaire < MARGE + HAUTEUR_PIED) {
                nouvellePage();
            }
        }

        void sousTitre(String texte) throws IOException {
            espace(8);
            assurerEspace(44);
            content.setNonStrokingColor(NAVY);
            content.addRect(MARGE, y - 3, 4, 14);
            content.fill();
            content.setNonStrokingColor(Color.BLACK);
            texteA(MARGE + 12, y, gras, 13, NAVY, texte);
            y -= 20;
        }

        void statTuiles(String[][] tuiles) throws IOException {
            float hauteurTuile = 54f;
            assurerEspace(hauteurTuile);
            float espaceEntreTuiles = 10f;
            float largeurTuile = (largeurUtile - espaceEntreTuiles * (tuiles.length - 1)) / tuiles.length;
            for (int i = 0; i < tuiles.length; i++) {
                float x = MARGE + i * (largeurTuile + espaceEntreTuiles);
                content.setNonStrokingColor(GRIS_FOND_TUILE);
                content.addRect(x, y - hauteurTuile, largeurTuile, hauteurTuile);
                content.fill();
                content.setStrokingColor(GRIS_BORDURE);
                content.setLineWidth(0.75f);
                content.addRect(x, y - hauteurTuile, largeurTuile, hauteurTuile);
                content.stroke();

                texteA(x + 10, y - 24, gras, 20, NAVY, tuiles[i][0]);
                texteA(x + 10, y - 40, normal, 8, GRIS_TEXTE, tuiles[i][1]);
            }
            y -= hauteurTuile;
        }

        void ligneListe(String texte) throws IOException {
            ecrireMultiligne(gras, 10, Color.BLACK, "- " + texte, 0f);
        }

        void ligneDetail(String texte) throws IOException {
            ecrireMultiligne(normal, 9, GRIS_TEXTE, texte, 12f);
        }

        /** Ligne de détail mise en évidence — formateur sans cours affecté. */
        void ligneDetailAlerte(String texte) throws IOException {
            ecrireMultiligne(gras, 9, ROUGE_ALERTE, texte, 12f);
        }

        void texteSimple(PDType1Font font, float taille, Color couleur, String texte) throws IOException {
            assurerEspace(taille + 6);
            texteA(MARGE, y, font, taille, couleur, texte);
            y -= taille + 6;
        }

        void espace(float hauteur) {
            y -= hauteur;
        }

        private void texteA(float x, float yAbs, PDType1Font font, float taille, Color couleur, String texte) throws IOException {
            content.setNonStrokingColor(couleur);
            content.beginText();
            content.setFont(font, taille);
            content.newLineAtOffset(x, yAbs);
            content.showText(texteSur(texte));
            content.endText();
            content.setNonStrokingColor(Color.BLACK);
        }

        private float largeurTexte(PDType1Font font, float taille, String texte) throws IOException {
            return font.getStringWidth(texteSur(texte)) / 1000 * taille;
        }

        private void ecrireMultiligne(PDType1Font font, float taille, Color couleur, String texte, float retrait)
                throws IOException {
            float largeurMax = largeurUtile - retrait;
            for (String ligne : decouperEnLignes(texteSur(texte), font, taille, largeurMax)) {
                assurerEspace(taille + 6);
                texteA(MARGE + retrait, y, font, taille, couleur, ligne);
                y -= taille + 6;
            }
        }

        private List<String> decouperEnLignes(String texte, PDType1Font font, float taille, float largeurMax)
                throws IOException {
            List<String> lignes = new ArrayList<>();
            StringBuilder ligneCourante = new StringBuilder();
            for (String mot : texte.split(" ")) {
                String essai = ligneCourante.isEmpty() ? mot : ligneCourante + " " + mot;
                if (font.getStringWidth(essai) / 1000 * taille > largeurMax && !ligneCourante.isEmpty()) {
                    lignes.add(ligneCourante.toString());
                    ligneCourante = new StringBuilder(mot);
                } else {
                    ligneCourante = new StringBuilder(essai);
                }
            }
            if (!ligneCourante.isEmpty()) {
                lignes.add(ligneCourante.toString());
            }
            if (lignes.isEmpty()) {
                lignes.add("");
            }
            return lignes;
        }

        void fermerAvecPiedDePage() throws IOException {
            content.close();
            int total = document.getNumberOfPages();
            for (int i = 0; i < total; i++) {
                PDPage p = document.getPage(i);
                try (PDPageContentStream pied = new PDPageContentStream(document, p,
                        PDPageContentStream.AppendMode.APPEND, true, true)) {
                    pied.setStrokingColor(GRIS_BORDURE);
                    pied.setLineWidth(0.5f);
                    pied.moveTo(MARGE, MARGE - 6);
                    pied.lineTo(largeurPage - MARGE, MARGE - 6);
                    pied.stroke();

                    pied.setNonStrokingColor(GRIS_TEXTE);
                    pied.beginText();
                    pied.setFont(normal, 8);
                    pied.newLineAtOffset(MARGE, MARGE - 18);
                    pied.showText(texteSur(titreRapport));
                    pied.endText();

                    String pagination = "Page " + (i + 1) + " / " + total;
                    float largeurPagination = normal.getStringWidth(texteSur(pagination)) / 1000 * 8;
                    pied.beginText();
                    pied.setFont(normal, 8);
                    pied.newLineAtOffset(largeurPage - MARGE - largeurPagination, MARGE - 18);
                    pied.showText(texteSur(pagination));
                    pied.endText();
                }
            }
        }
    }
}
