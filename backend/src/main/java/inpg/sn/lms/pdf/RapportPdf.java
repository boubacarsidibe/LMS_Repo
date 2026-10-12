package inpg.sn.lms.pdf;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.util.Matrix;

/**
 * Moteur de mise en page des rapports PDF de l'INPG : une « machine à écrire verticale » A4 sur le
 * {@link PapierEnTete} de l'institut (logo, pied de page, pagination), avec retour à la ligne et saut de page
 * automatiques.
 *
 * <p>Il remplace trois copies quasi identiques d'un même petit moteur (rapports qualité, activités du personnel,
 * comptes formateurs), qui partageaient deux défauts :
 * <ul>
 *   <li>un filet tracé <em>à mi-hauteur</em> du titre de section, donc « barrant » le texte : ici le filet est
 *       toujours sous la ligne de base ;</li>
 *   <li>un texte trop long coupé par « ... » : le contenu était perdu. Ici il passe à la ligne, sans limite.</li>
 * </ul>
 *
 * <p>Usage : {@code try (RapportPdf rapport = new RapportPdf("Titre")) { rapport.titre(...); ...; return rapport.generer(); }}.
 */
public final class RapportPdf implements AutoCloseable {

    /** Air supplémentaire entre le logo et le début du texte (le papier à en-tête seul ne laissait que 16 pt). */
    private static final float ESPACE_SOUS_LOGO = 22f;

    private static final Color NAVY = new Color(19, 23, 65);
    private static final Color GRIS = new Color(70, 70, 78);
    private static final Color TEXTE = new Color(30, 30, 36);
    private static final Color ALERTE = new Color(179, 38, 30);
    private static final Color TUILE_FOND = new Color(247, 248, 250);
    private static final Color TUILE_BORDURE = new Color(224, 226, 229);

    private static final float INTERLIGNE = 1.35f;

    private final PDDocument document = new PDDocument();
    private final PolicesPdf polices;
    private final String titreDocument;
    private final float largeurPage = PDRectangle.A4.getWidth();
    private final float hauteurPage = PDRectangle.A4.getHeight();
    private final float largeurUtile = largeurPage - PapierEnTete.MARGE_GAUCHE - PapierEnTete.MARGE_DROITE;

    private PDPageContentStream flux;
    /** Texte répété en filigrane sur chaque page (référence d'un document officiel), ou {@code null}. */
    private String filigrane;
    /** Ordonnée du haut de la prochaine ligne. */
    private float y;

    public RapportPdf(String titreDocument) throws IOException {
        this.titreDocument = titreDocument;
        this.polices = new PolicesPdf(document);
        nouvellePage();
    }

    // ===================== Éléments =====================

    /** Titre du rapport (18 pt gras). */
    public void titre(String texte) throws IOException {
        paragraphe(polices.gras, 18, NAVY, texte, 0f, 0f, 4f);
    }

    /** Ligne d'information sous le titre : date de génération, période, établissement. */
    public void meta(String texte) throws IOException {
        paragraphe(polices.italique, 9, GRIS, texte, 0f, 0f, 1f);
    }

    public void espace(float hauteur) {
        y -= hauteur;
    }

    /**
     * Garantit {@code hauteur} points libres à partir d'ici, quitte à passer à la page suivante. À poser devant un
     * ensemble qui doit rester d'un seul tenant (une synthèse et sa signature, par exemple).
     */
    public void reserver(float hauteur) throws IOException {
        assurerEspace(hauteur);
    }

    /**
     * Titre de section : texte puis filet <strong>en dessous</strong>. Reste solidaire du contenu qui suit (une
     * section ne se retrouve jamais seule en bas de page).
     */
    public void section(String texte) throws IOException {
        assurerEspace(60f);
        y -= 8f;
        float taille = 13f;
        String ligne = PolicesPdf.sur(polices.gras, texte);
        ecrire(polices.gras, taille, NAVY, ligne, PapierEnTete.MARGE_GAUCHE, y - taille);
        float filet = y - taille - 5f;
        flux.setStrokingColor(NAVY);
        flux.setLineWidth(0.8f);
        flux.moveTo(PapierEnTete.MARGE_GAUCHE, filet);
        flux.lineTo(PapierEnTete.MARGE_GAUCHE + largeurUtile, filet);
        flux.stroke();
        y = filet - 8f;
    }

    /** Intertitre (11 pt gras), sans filet : regroupe des éléments d'une section. */
    public void intertitre(String texte) throws IOException {
        assurerEspace(40f);
        paragraphe(polices.gras, 11, NAVY, texte, 0f, 4f, 1f);
    }

    /** « Libellé : valeur » (10,5 pt). */
    public void ligneCle(String libelle, String valeur) throws IOException {
        paragraphe(polices.regulier, 10.5f, TEXTE, libelle + " : " + valeur, 0f, 0f, 2f);
    }

    /** Élément de liste, en gras, avec puce et retrait suspendu. Reste solidaire de sa première ligne de détail. */
    public void puce(String texte) throws IOException {
        float taille = 10f;
        int lignes = couper(polices.gras, taille, texte, largeurUtile - 10f).size();
        assurerEspace(lignes * taille * INTERLIGNE + 3 * 9 * INTERLIGNE);
        paragraphe(polices.gras, taille, TEXTE, texte, 10f, 0f, 1f, "•");
    }

    /** Texte d'appoint (9 pt) en retrait sous un élément de liste. */
    public void detail(String texte) throws IOException {
        paragraphe(polices.regulier, 9, GRIS, texte, 14f, 0f, 1f);
    }

    public void detailItalique(String texte) throws IOException {
        paragraphe(polices.italique, 9, GRIS, texte, 14f, 0f, 1f);
    }

    /** Détail signalant un problème, en rouge. */
    public void detailAlerte(String texte) throws IOException {
        paragraphe(polices.italique, 9, ALERTE, texte, 14f, 0f, 1f);
    }

    /** Paragraphe courant (10 pt). */
    public void texte(String texte) throws IOException {
        paragraphe(polices.regulier, 10, TEXTE, texte, 0f, 0f, 2f);
    }

    public void texteItalique(String texte) throws IOException {
        paragraphe(polices.italique, 10, GRIS, texte, 0f, 0f, 2f);
    }

    /**
     * Bloc de signature, aligné à droite : « Fait à …, le … », la fonction du signataire, puis un espace laissé
     * libre pour la signature et le cachet. Le bloc ne se coupe jamais entre deux pages : s'il ne tient pas, il passe
     * entier à la page suivante plutôt que de laisser la fonction sans sa place de signature.
     */
    public void blocSignature(String lieuEtDate, String fonction) throws IOException {
        final float espaceSignatureEtCachet = 70f;
        final float x = PapierEnTete.MARGE_GAUCHE + largeurUtile * 0.5f;
        final float largeurBloc = largeurUtile * 0.5f;
        assurerEspace(60f + espaceSignatureEtCachet);
        y -= 16f;
        float hauteurLigne = 10f * INTERLIGNE;
        for (String ligne : couper(polices.regulier, 10, PolicesPdf.sur(polices.regulier, lieuEtDate), largeurBloc)) {
            ecrire(polices.regulier, 10, TEXTE, ligne, x, y - 10f);
            y -= hauteurLigne;
        }
        y -= 2f;
        for (String ligne : couper(polices.gras, 10, PolicesPdf.sur(polices.gras, fonction), largeurBloc)) {
            ecrire(polices.gras, 10, NAVY, ligne, x, y - 10f);
            y -= hauteurLigne;
        }
        y -= espaceSignatureEtCachet;
    }

    /** Rangée de tuiles de chiffres clés : {@code { { "2", "Formateurs" }, ... }}. */
    public void tuiles(String[][] tuiles) throws IOException {
        float hauteurTuile = 50f;
        float ecart = 12f;
        assurerEspace(hauteurTuile + 4f);
        float largeurTuile = (largeurUtile - ecart * (tuiles.length - 1)) / tuiles.length;
        float x = PapierEnTete.MARGE_GAUCHE;
        for (String[] tuile : tuiles) {
            flux.setNonStrokingColor(TUILE_FOND);
            flux.addRect(x, y - hauteurTuile, largeurTuile, hauteurTuile);
            flux.fill();
            flux.setStrokingColor(TUILE_BORDURE);
            flux.setLineWidth(0.75f);
            flux.addRect(x, y - hauteurTuile, largeurTuile, hauteurTuile);
            flux.stroke();
            ecrire(polices.gras, 20, NAVY, PolicesPdf.sur(polices.gras, tuile[0]), x + 10, y - 26);
            ecrire(polices.regulier, 8, GRIS, PolicesPdf.sur(polices.regulier, tuile[1]), x + 10, y - 41);
            x += largeurTuile + ecart;
        }
        y -= hauteurTuile + 8f;
    }

    /**
     * Tableau à en-tête coloré, lignes alternées et cellules qui passent à la ligne. L'en-tête est répété si le
     * tableau change de page ; une ligne n'est jamais coupée en deux.
     *
     * @param parts largeurs relatives des colonnes, par exemple {@code { 1, 4, 1, 2 }}
     */
    public void tableau(String[] entetes, float[] parts, List<String[]> lignes) throws IOException {
        float somme = 0;
        for (float part : parts) {
            somme += part;
        }
        float[] largeurs = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            largeurs[i] = largeurUtile * parts[i] / somme;
        }
        float taille = 9.5f;
        float hauteurLigne = taille * INTERLIGNE;

        assurerEspace(hauteurLigne + 8f + 3 * hauteurLigne);
        enteteTableau(entetes, largeurs, taille);
        int index = 0;
        for (String[] ligne : lignes) {
            List<List<String>> cellules = new ArrayList<>();
            int maxLignes = 1;
            for (int c = 0; c < largeurs.length; c++) {
                List<String> coupees = couper(polices.regulier, taille, c < ligne.length ? ligne[c] : "", largeurs[c] - 12f);
                cellules.add(coupees);
                maxLignes = Math.max(maxLignes, coupees.size());
            }
            float hauteur = maxLignes * hauteurLigne + 8f;
            if (y - hauteur < PapierEnTete.BAS_CONTENU) {
                nouvellePage();
                enteteTableau(entetes, largeurs, taille);
            }
            if (index % 2 == 1) {
                flux.setNonStrokingColor(TUILE_FOND);
                flux.addRect(PapierEnTete.MARGE_GAUCHE, y - hauteur, largeurUtile, hauteur);
                flux.fill();
            }
            flux.setStrokingColor(TUILE_BORDURE);
            flux.setLineWidth(0.5f);
            flux.moveTo(PapierEnTete.MARGE_GAUCHE, y - hauteur);
            flux.lineTo(PapierEnTete.MARGE_GAUCHE + largeurUtile, y - hauteur);
            flux.stroke();
            float x = PapierEnTete.MARGE_GAUCHE;
            for (int c = 0; c < largeurs.length; c++) {
                float ligneY = y - 4f - taille;
                for (String morceau : cellules.get(c)) {
                    ecrire(polices.regulier, taille, TEXTE, morceau, x + 6f, ligneY);
                    ligneY -= hauteurLigne;
                }
                x += largeurs[c];
            }
            y -= hauteur;
            index++;
        }
        y -= 6f;
    }

    private void enteteTableau(String[] entetes, float[] largeurs, float taille) throws IOException {
        float hauteur = taille * INTERLIGNE + 8f;
        flux.setNonStrokingColor(NAVY);
        flux.addRect(PapierEnTete.MARGE_GAUCHE, y - hauteur, largeurUtile, hauteur);
        flux.fill();
        float x = PapierEnTete.MARGE_GAUCHE;
        for (int c = 0; c < largeurs.length; c++) {
            ecrire(polices.gras, taille, Color.WHITE, PolicesPdf.sur(polices.gras, entetes[c]), x + 6f, y - 4f - taille);
            x += largeurs[c];
        }
        y -= hauteur;
    }

    /**
     * Filigrane discret (texte répété en diagonale, 6 % d'opacité) sur toutes les pages, présentes et à venir :
     * limite la contrefaçon d'un document officiel.
     */
    public void filigrane(String texte) throws IOException {
        this.filigrane = texte;
        dessinerFiligrane();
    }

    private void dessinerFiligrane() throws IOException {
        if (filigrane == null) {
            return;
        }
        PDExtendedGraphicsState transparent = new PDExtendedGraphicsState();
        transparent.setNonStrokingAlphaConstant(0.06f);
        flux.saveGraphicsState();
        flux.setGraphicsStateParameters(transparent);
        flux.setNonStrokingColor(Color.BLACK);
        flux.beginText();
        flux.setFont(polices.gras, 26);
        String ligne = PolicesPdf.sur(polices.gras, ("INPG - " + filigrane + "   ").repeat(4));
        for (int i = 0; i < 8; i++) {
            flux.setTextMatrix(Matrix.getRotateInstance(Math.toRadians(30), -60, i * 120 - 30));
            flux.showText(ligne);
        }
        flux.endText();
        flux.restoreGraphicsState();
    }

    // ===================== Sortie =====================

    /** Pose le papier à en-tête sur toutes les pages puis renvoie le PDF. */
    public byte[] generer() throws IOException {
        flux.close();
        flux = null;
        PapierEnTete.apposer(document, polices);
        document.getDocumentInformation().setTitle(titreDocument);
        document.getDocumentInformation().setAuthor("Institut National du Pétrole et du Gaz");
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        document.save(sortie);
        return sortie.toByteArray();
    }

    @Override
    public void close() throws IOException {
        if (flux != null) {
            flux.close();
        }
        document.close();
    }

    // ===================== Mise en page =====================

    private void nouvellePage() throws IOException {
        if (flux != null) {
            flux.close();
        }
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        flux = new PDPageContentStream(document, page);
        y = hauteurPage - PapierEnTete.HAUT_CONTENU - ESPACE_SOUS_LOGO;
        dessinerFiligrane();
    }

    private void assurerEspace(float hauteur) throws IOException {
        if (y - hauteur < PapierEnTete.BAS_CONTENU) {
            nouvellePage();
        }
    }

    private void paragraphe(PDFont police, float taille, Color couleur, String texte, float retrait, float avant,
            float apres) throws IOException {
        paragraphe(police, taille, couleur, texte, retrait, avant, apres, null);
    }

    /**
     * Écrit un paragraphe sur autant de lignes que nécessaire, en changeant de page au besoin. Avec une puce, elle
     * s'écrit devant la première ligne et les suivantes s'alignent sur le texte.
     */
    private void paragraphe(PDFont police, float taille, Color couleur, String texte, float retrait, float avant,
            float apres, String puce) throws IOException {
        y -= avant;
        List<String> lignes = couper(police, taille, texte, largeurUtile - retrait);
        float hauteurLigne = taille * INTERLIGNE;
        boolean premiere = true;
        for (String ligne : lignes) {
            assurerEspace(hauteurLigne);
            if (premiere && puce != null) {
                ecrire(police, taille, couleur, PolicesPdf.sur(police, puce), PapierEnTete.MARGE_GAUCHE, y - taille);
            }
            ecrire(police, taille, couleur, ligne, PapierEnTete.MARGE_GAUCHE + retrait, y - taille);
            y -= hauteurLigne;
            premiere = false;
        }
        y -= apres;
    }

    private void ecrire(PDFont police, float taille, Color couleur, String texte, float x, float ligneDeBase)
            throws IOException {
        flux.beginText();
        flux.setNonStrokingColor(couleur);
        flux.setFont(police, taille);
        flux.newLineAtOffset(x, ligneDeBase);
        flux.showText(texte);
        flux.endText();
    }

    /**
     * Découpe un texte en lignes de largeur au plus {@code largeurMax}. Les retours à la ligne du texte sont
     * respectés ; un mot plus large qu'une ligne est coupé au caractère plutôt que de déborder.
     */
    static List<String> couper(PDFont police, float taille, String texte, float largeurMax) throws IOException {
        List<String> lignes = new ArrayList<>();
        for (String paragraphe : (texte == null ? "" : texte).split("\\R", -1)) {
            String propre = PolicesPdf.sur(police, paragraphe).strip();
            if (propre.isEmpty()) {
                lignes.add("");
                continue;
            }
            StringBuilder courante = new StringBuilder();
            for (String mot : propre.split(" +")) {
                String essai = courante.isEmpty() ? mot : courante + " " + mot;
                if (largeur(police, taille, essai) <= largeurMax) {
                    courante.setLength(0);
                    courante.append(essai);
                    continue;
                }
                if (!courante.isEmpty()) {
                    lignes.add(courante.toString());
                    courante.setLength(0);
                }
                // mot seul plus large que la ligne : coupe au caractère
                StringBuilder morceau = new StringBuilder();
                for (char c : mot.toCharArray()) {
                    if (largeur(police, taille, morceau.toString() + c) > largeurMax && !morceau.isEmpty()) {
                        lignes.add(morceau.toString());
                        morceau.setLength(0);
                    }
                    morceau.append(c);
                }
                courante.append(morceau);
            }
            lignes.add(courante.toString());
        }
        return lignes;
    }

    private static float largeur(PDFont police, float taille, String texte) throws IOException {
        return police.getStringWidth(texte) / 1000 * taille;
    }
}
