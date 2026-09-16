package inpg.sn.lms.recrutement.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDate;
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

import inpg.sn.lms.recrutement.dto.RapportRecrutementDto;

/**
 * Rapport qualité du recrutement, en PDF : répartition des candidatures par
 * étape du workflow avec la liste nominative de chacune (TDR §A). Même patron
 * que {@code RapportQualitePdfService} (suivi qualité pédagogique) — dupliqué
 * plutôt que partagé, comme {@code RapportActivitePdfService} l'est déjà pour
 * la même raison : chaque rapport PDF de ce dépôt reste un petit module
 * autonome, sans dépendance croisée entre domaines métier.
 *
 * <p>Aucun chiffre n'est recalculé ici : le service met seulement en page le
 * {@link RapportRecrutementDto} déjà produit par
 * {@code CandidatureService#genererRapportParEtape}.
 *
 * <p>Bandeau de marque, tableau des chiffres clés et pied de page paginé sont
 * volontairement dessinés à la main (PDFBox seul, pas de bibliothèque de mise
 * en page) : ce module reste cohérent avec le reste de ce dépôt, qui n'a
 * jamais introduit de dépendance de rendu au-delà de PDFBox/OpenPDF.
 */
@Service
public class RapportRecrutementPdfService {

    private static final float MARGE = 50f;
    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMAT_DATE_HEURE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    /** Encodage réellement utilisé par Helvetica dans PDFBox (accents, tirets longs, guillemets typographiques...). */
    private static final Charset WINANSI = Charset.forName("windows-1252");

    /** Bleu institutionnel INPG — même valeur que le jeton `brand` du frontend (#131741). */
    private static final Color NAVY = new Color(19, 23, 65);
    private static final Color NAVY_CLAIR = new Color(212, 227, 255);
    private static final Color GRIS_TEXTE = new Color(70, 70, 78);
    private static final Color GRIS_LIGNE_ALT = new Color(247, 248, 250);
    private static final Color GRIS_BORDURE = new Color(224, 226, 229);

    /** Suffixe de date pour le nom de fichier : {@code AAAA-MM-JJ}. */
    public String suffixeDuJour() {
        return LocalDate.now().toString();
    }

    public byte[] genererRapport(RapportRecrutementDto rapport) {
        String genereLe = "Généré le " + LocalDateTime.now().format(FORMAT_DATE_HEURE);
        try (PDDocument document = new PDDocument()) {
            Contexte ctx = new Contexte(document, "Rapport qualité — Recrutement", genereLe);

            ctx.sousTitre("Chiffres clés");
            ctx.statTotal(rapport.totalCandidatures());
            ctx.espace(10);
            ctx.tableauEtapes(rapport.etapes(), rapport.totalCandidatures());
            ctx.espace(18);

            for (RapportRecrutementDto.EtapeRapport etape : rapport.etapes()) {
                ctx.sousTitre(etape.libelle() + " (" + etape.nombre() + ")");
                if (etape.candidats().isEmpty()) {
                    ctx.texte(ctx.italique, 10, GRIS_TEXTE, "Aucune candidature à cette étape.");
                    ctx.espace(10);
                    continue;
                }
                for (RapportRecrutementDto.CandidatRapport c : etape.candidats()) {
                    String entete = (c.numeroDossier() == null ? "" : c.numeroDossier() + " — ")
                            + nvl(c.nom()) + " " + nvl(c.prenom());
                    ctx.ligneListe(entete);
                    String detail = nvl(c.email())
                            + (c.sessionLibelle() != null ? "  |  Session : " + c.sessionLibelle() : "")
                            + (c.dateSoumission() != null ? "  |  Déposé le " + c.dateSoumission().format(FORMAT_DATE) : "");
                    ctx.ligneDetail(detail);
                    if (c.motifRejet() != null && !c.motifRejet().isBlank()) {
                        ctx.ligneDetail("Motif du rejet : " + c.motifRejet());
                    }
                }
                ctx.espace(10);
            }

            ctx.fermerAvecPiedDePage();
            return octets(document);
        } catch (Exception exception) {
            throw new RuntimeException("La génération du rapport qualité recrutement a échoué.", exception);
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

    /**
     * Rend un texte sûr pour l'encodage WinAnsi de Helvetica : tout caractère
     * hors Latin-1 est remplacé par '?', ce qui évite un
     * {@code IllegalArgumentException} en pleine génération.
     */
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

    /**
     * Machine à écrire verticale avec bandeau de marque, saut de page
     * automatique et pied de page paginé.
     */
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
            // Réserve aussi la place d'au moins une ligne de contenu en dessous : un
            // intertitre seul en bas de page, sans rien en dessous, est aussi peu lisible
            // qu'une erreur de saisie — mieux vaut le reporter en haut de la page suivante.
            assurerEspace(44);
            content.setNonStrokingColor(NAVY);
            content.addRect(MARGE, y - 3, 4, 14);
            content.fill();
            content.setNonStrokingColor(Color.BLACK);
            texteA(MARGE + 12, y, gras, 13, NAVY, texte);
            y -= 20;
        }

        /** Mise en avant du total, en gros caractères, avant le détail par étape. */
        void statTotal(int total) throws IOException {
            assurerEspace(30);
            texteA(MARGE, y, gras, 24, NAVY, String.valueOf(total));
            float largeurNombre = largeurTexte(gras, 24, String.valueOf(total));
            texteA(MARGE + largeurNombre + 8, y - 15, normal, 10, GRIS_TEXTE, "candidature(s) au total, toutes sessions confondues");
            y -= 30;
        }

        /** Tableau « Étape | Candidatures » avec lignes alternées, plutôt qu'une liste de texte brut. */
        void tableauEtapes(List<RapportRecrutementDto.EtapeRapport> etapes, int total) throws IOException {
            float hauteurLigne = 20f;
            float largeurColNombre = 130f;
            float largeurColEtape = largeurUtile - largeurColNombre;

            assurerEspace(hauteurLigne);
            content.setNonStrokingColor(NAVY);
            content.addRect(MARGE, y - hauteurLigne + 5, largeurUtile, hauteurLigne);
            content.fill();
            texteA(MARGE + 8, y - hauteurLigne + 10, gras, 10, Color.WHITE, "Étape du workflow");
            texteA(MARGE + largeurColEtape + 8, y - hauteurLigne + 10, gras, 10, Color.WHITE, "Candidatures");
            y -= hauteurLigne;

            int index = 0;
            for (RapportRecrutementDto.EtapeRapport etape : etapes) {
                assurerEspace(hauteurLigne);
                if (index % 2 == 1) {
                    content.setNonStrokingColor(GRIS_LIGNE_ALT);
                    content.addRect(MARGE, y - hauteurLigne + 5, largeurUtile, hauteurLigne);
                    content.fill();
                }
                content.setStrokingColor(GRIS_BORDURE);
                content.setLineWidth(0.5f);
                content.moveTo(MARGE, y - hauteurLigne + 5);
                content.lineTo(MARGE + largeurUtile, y - hauteurLigne + 5);
                content.stroke();

                texteA(MARGE + 8, y - hauteurLigne + 10, normal, 10, Color.BLACK, etape.libelle());
                String valeur = etape.nombre() + tauxEntreParentheses(etape.nombre(), total);
                texteA(MARGE + largeurColEtape + 8, y - hauteurLigne + 10, normal, 10, Color.BLACK, valeur);
                y -= hauteurLigne;
                index++;
            }
        }

        private String tauxEntreParentheses(int nombre, int total) {
            if (total <= 0) {
                return "";
            }
            return " (" + Math.round(100.0 * nombre / total) + " %)";
        }

        void ligneListe(String texte) throws IOException {
            ecrireMultiligne(gras, 10, Color.BLACK, "- " + texte, 0f);
        }

        /** Ligne d'appoint, en retrait sous un élément de liste. */
        void ligneDetail(String texte) throws IOException {
            ecrireMultiligne(normal, 9, GRIS_TEXTE, texte, 12f);
        }

        void texte(PDType1Font font, float taille, Color couleur, String texte) throws IOException {
            ecrireMultiligne(font, taille, couleur, texte, 0f);
        }

        void espace(float hauteur) {
            y -= hauteur;
        }

        /** Texte positionné librement (pas le flux vertical courant) — utilisé pour le bandeau et les tableaux. */
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

        /**
         * Découpe {@code texte} en autant de lignes que nécessaire pour tenir dans
         * la largeur utile, plutôt que de le tronquer — un nom, un courriel ou une
         * session un peu longs ne doivent pas disparaître du rapport.
         */
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

        /** Ferme le flux courant puis tamponne un pied de page paginé sur chaque page du document. */
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
