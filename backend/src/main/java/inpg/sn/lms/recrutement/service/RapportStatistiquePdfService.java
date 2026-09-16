package inpg.sn.lms.recrutement.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import inpg.sn.lms.recrutement.dto.RapportStatistiqueDto;

/**
 * Rapport statistique du recrutement, en PDF : mêmes chiffres que la page
 * Statistiques du portail admission, avec des barres de progression fidèles
 * à celles affichées à l'écran plutôt qu'un tableau brut. Même patron que
 * {@code RapportRecrutementPdfService} — dupliqué plutôt que partagé, comme
 * les autres rapports PDF de ce dépôt.
 *
 * <p>Aucun chiffre n'est recalculé ici : le service met seulement en page le
 * {@link RapportStatistiqueDto} déjà produit par
 * {@code CandidatureService#genererRapportStatistique}.
 */
@Service
public class RapportStatistiquePdfService {

    private static final float MARGE = 50f;
    private static final DateTimeFormatter FORMAT_DATE_HEURE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Charset WINANSI = Charset.forName("windows-1252");

    private static final Color NAVY = new Color(19, 23, 65);
    private static final Color GRIS_TEXTE = new Color(70, 70, 78);
    private static final Color GRIS_LIGNE_ALT = new Color(240, 241, 244);
    private static final Color GRIS_BORDURE = new Color(224, 226, 229);
    private static final Color GRIS_FOND_TUILE = new Color(247, 248, 250);

    public String suffixeDuJour() {
        return LocalDate.now().toString();
    }

    public byte[] genererRapport(RapportStatistiqueDto rapport) {
        String genereLe = "Généré le " + LocalDateTime.now().format(FORMAT_DATE_HEURE);
        try (PDDocument document = new PDDocument()) {
            Contexte ctx = new Contexte(document, "Rapport statistique — Recrutement", genereLe);

            ctx.statTuiles(new String[][] {
                    { String.valueOf(rapport.totalCandidats()), "Total candidats" },
                    { String.valueOf(rapport.validationEnCours()), "Validation en cours" },
                    { String.valueOf(rapport.preselectionnes()), "Présélectionnés" },
                    { String.valueOf(rapport.rejetes()), "Rejetés" },
            });
            ctx.espace(20);

            ctx.sousTitre("Répartition des candidatures par étape");
            if (rapport.totalCandidats() == 0) {
                ctx.texteSimple(ctx.italique, 10, GRIS_TEXTE, "Aucune candidature pour le moment.");
            } else {
                for (RapportStatistiqueDto.Segment seg : rapport.repartitionStatuts()) {
                    ctx.barre(seg.label(), seg.count(), seg.pourcentage());
                }
            }
            ctx.espace(14);

            ctx.sousTitre("Taux d'admission");
            ctx.statTotal(rapport.tauxAdmission() + " %",
                    rapport.candidaturesDecidees() > 0
                            ? rapport.candidaturesDecidees() + " dossier(s) tranché(s) (admis final ou rejeté)"
                            : "Aucune décision finale pour le moment");
            ctx.espace(20);

            ctx.sousTitre("Profil démographique — Genre");
            ctx.sousSection(rapport.repartitionGenre());

            ctx.sousTitre("Profil démographique — Tranche d'âge");
            ctx.sousSection(rapport.repartitionAge());

            ctx.sousTitre("Professions les plus déclarées");
            ctx.sousSection(rapport.topProfessions());
            if (rapport.nbSansProfessionDeclaree() > 0) {
                ctx.texteSimple(ctx.normal, 9, GRIS_TEXTE,
                        rapport.nbSansProfessionDeclaree() + " candidat(s) sans profession déclarée.");
            }
            ctx.espace(10);

            ctx.sousTitre("Nationalités");
            ctx.sousSection(rapport.topNationalites());

            ctx.fermerAvecPiedDePage();
            return octets(document);
        } catch (Exception exception) {
            throw new RuntimeException("La génération du rapport statistique recrutement a échoué.", exception);
        }
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

        /** Rangée de tuiles chiffrées (KPI), à l'image des cartes du tableau de bord. */
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

        /** Un seul gros chiffre, avec sa légende juste en dessous. */
        void statTotal(String valeur, String legende) throws IOException {
            assurerEspace(44);
            texteA(MARGE, y, gras, 26, NAVY, valeur);
            y -= 30;
            texteA(MARGE, y, normal, 10, GRIS_TEXTE, legende);
            y -= 14;
        }

        void sousSection(List<RapportStatistiqueDto.Segment> segments) throws IOException {
            if (segments.isEmpty()) {
                texteSimple(italique, 10, GRIS_TEXTE, "Aucune donnée.");
                return;
            }
            for (RapportStatistiqueDto.Segment seg : segments) {
                barre(seg.label(), seg.count(), seg.pourcentage());
            }
        }

        /** Étiquette + décompte à droite, puis une barre de progression en dessous — comme à l'écran. */
        void barre(String label, int count, int pourcentage) throws IOException {
            assurerEspace(28);
            texteA(MARGE, y, normal, 9, Color.BLACK, label);
            String valeurTexte = count + " (" + pourcentage + " %)";
            float largeurValeur = largeurTexte(normal, 9, valeurTexte);
            texteA(MARGE + largeurUtile - largeurValeur, y, normal, 9, GRIS_TEXTE, valeurTexte);
            y -= 10;

            content.setNonStrokingColor(GRIS_LIGNE_ALT);
            content.addRect(MARGE, y - 6, largeurUtile, 6);
            content.fill();
            float largeurRemplie = largeurUtile * Math.max(0, Math.min(100, pourcentage)) / 100f;
            if (largeurRemplie > 0) {
                content.setNonStrokingColor(NAVY);
                content.addRect(MARGE, y - 6, largeurRemplie, 6);
                content.fill();
            }
            content.setNonStrokingColor(Color.BLACK);
            y -= 16;
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
