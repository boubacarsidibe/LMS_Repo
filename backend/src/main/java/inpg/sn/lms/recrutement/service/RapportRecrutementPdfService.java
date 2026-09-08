package inpg.sn.lms.recrutement.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
 */
@Service
public class RapportRecrutementPdfService {

    private static final float MARGE = 50f;
    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMAT_DATE_HEURE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    /** Encodage réellement utilisé par Helvetica dans PDFBox (accents, tirets longs, guillemets typographiques...). */
    private static final Charset WINANSI = Charset.forName("windows-1252");

    /** Suffixe de date pour le nom de fichier : {@code AAAA-MM-JJ}. */
    public String suffixeDuJour() {
        return LocalDate.now().toString();
    }

    public byte[] genererRapport(RapportRecrutementDto rapport) {
        try (PDDocument document = new PDDocument()) {
            Contexte ctx = new Contexte(document);

            ctx.titre("Rapport qualité — Recrutement");
            ctx.texte(ctx.italique, 10, "Institut National du Pétrole et du Gaz");
            ctx.texte(ctx.italique, 10, "Généré le " + LocalDateTime.now().format(FORMAT_DATE_HEURE));
            ctx.espace(16);

            ctx.sousTitre("Chiffres clés");
            ctx.ligneKpi("Total des candidatures", String.valueOf(rapport.totalCandidatures()));
            for (RapportRecrutementDto.EtapeRapport etape : rapport.etapes()) {
                ctx.ligneKpi(etape.libelle(), etape.nombre() + tauxEntreParentheses(etape.nombre(), rapport.totalCandidatures()));
            }
            ctx.espace(16);

            for (RapportRecrutementDto.EtapeRapport etape : rapport.etapes()) {
                ctx.sousTitre(etape.libelle() + " (" + etape.nombre() + ")");
                if (etape.candidats().isEmpty()) {
                    ctx.texte(ctx.italique, 10, "Aucune candidature à cette étape.");
                    ctx.espace(8);
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

            ctx.fermer();
            return octets(document);
        } catch (Exception exception) {
            throw new RuntimeException("La génération du rapport qualité recrutement a échoué.", exception);
        }
    }

    private String tauxEntreParentheses(int nombre, int total) {
        if (total <= 0) {
            return "";
        }
        return " (" + Math.round(100.0 * nombre / total) + " %)";
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

    /** Petite machine à écrire verticale avec saut de page automatique. */
    private static final class Contexte {
        private final PDDocument document;
        private PDPage page;
        private PDPageContentStream content;
        private float y;
        private final float largeurUtile;

        final PDType1Font normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        final PDType1Font gras = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        final PDType1Font italique = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

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

        private void assurerEspace(float hauteurLigne) throws IOException {
            if (y - hauteurLigne < MARGE) {
                nouvellePage();
            }
        }

        void titre(String texte) throws IOException {
            assurerEspace(30);
            content.setNonStrokingColor(new Color(19, 23, 65));
            ecrire(gras, 20, texte);
            content.setNonStrokingColor(Color.BLACK);
            y -= 8;
        }

        void sousTitre(String texte) throws IOException {
            espace(6);
            assurerEspace(22);
            content.setStrokingColor(new Color(19, 23, 65));
            content.setLineWidth(1f);
            content.moveTo(MARGE, y + 4);
            content.lineTo(MARGE + largeurUtile, y + 4);
            content.stroke();
            ecrire(gras, 14, texte);
        }

        void ligneKpi(String libelle, String valeur) throws IOException {
            assurerEspace(16);
            ecrire(normal, 11, libelle + " : " + valeur);
        }

        void ligneListe(String texte) throws IOException {
            assurerEspace(16);
            ecrire(gras, 10, "- " + texte);
        }

        /** Ligne d'appoint, en retrait sous un élément de liste. */
        void ligneDetail(String texte) throws IOException {
            assurerEspace(14);
            ecrireIndente(normal, 9, texte, 12f);
        }

        void texte(PDType1Font font, float taille, String texte) throws IOException {
            assurerEspace(16);
            ecrire(font, taille, texte);
        }

        void espace(float hauteur) {
            y -= hauteur;
        }

        private void ecrire(PDType1Font font, float taille, String texte) throws IOException {
            ecrireIndente(font, taille, texte, 0f);
        }

        private void ecrireIndente(PDType1Font font, float taille, String texte, float retrait) throws IOException {
            content.beginText();
            content.setFont(font, taille);
            content.newLineAtOffset(MARGE + retrait, y);
            content.showText(tronquer(texteSur(texte), font, taille, largeurUtile - retrait));
            content.endText();
            y -= taille + 6;
        }

        /** Coupe le texte plutôt que de le laisser déborder de la page. */
        private String tronquer(String texte, PDType1Font font, float taille, float largeurMax) throws IOException {
            if (font.getStringWidth(texte) / 1000 * taille <= largeurMax) {
                return texte;
            }
            String suffixe = "...";
            StringBuilder resultat = new StringBuilder();
            for (char c : texte.toCharArray()) {
                String essai = resultat.toString() + c + suffixe;
                if (font.getStringWidth(essai) / 1000 * taille > largeurMax) {
                    break;
                }
                resultat.append(c);
            }
            return resultat + suffixe;
        }

        void fermer() throws IOException {
            content.close();
        }
    }
}
