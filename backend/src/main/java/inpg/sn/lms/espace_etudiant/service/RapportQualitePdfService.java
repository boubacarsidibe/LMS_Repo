package inpg.sn.lms.espace_etudiant.service;

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

import inpg.sn.lms.espace_etudiant.dto.QualiteCoursDto;
import inpg.sn.lms.espace_etudiant.dto.SyntheseEnqueteSuiviDto;

/**
 * Rapports du suivi qualité, en PDF (TDR §G, besoin B5).
 *
 * <p>Deux rapports <strong>distincts</strong>, chacun téléchargeable
 * séparément : les avis des étudiants sur les cours d'une part, l'impact de la
 * formation sur la carrière des diplômés d'autre part. Ce sont deux lectures,
 * deux publics et deux temporalités — les mélanger dans un seul document rendait
 * chacun moins exploitable.
 *
 * <p>Aucun chiffre n'est recalculé ici : le service ne fait que mettre en page
 * les {@link QualiteCoursDto} / {@link SyntheseEnqueteSuiviDto} déjà produits
 * pour l'écran « Suivi qualité », pour ne jamais en diverger.
 */
@Service
public class RapportQualitePdfService {

    private static final float MARGE = 50f;
    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    /** Encodage réellement utilisé par Helvetica dans PDFBox (accents, tirets longs, guillemets typographiques...). */
    private static final Charset WINANSI = Charset.forName("windows-1252");

    /** Suffixe de date pour les noms de fichiers : {@code AAAA-MM-JJ}. */
    public String suffixeDuJour() {
        return LocalDate.now().toString();
    }

    // ======================================================================
    // Rapport 1 — Avis des étudiants sur les cours
    // ======================================================================

    public byte[] genererRapportAvisCours(QualiteCoursDto qualite) {
        try (PDDocument document = new PDDocument()) {
            Contexte ctx = new Contexte(document);

            ctx.titre("Rapport qualité — Avis sur les cours");
            ctx.texte(ctx.italique, 10, "Institut National du Pétrole et du Gaz");
            ctx.texte(ctx.italique, 10, "Généré le " + LocalDateTime.now().format(FORMAT_DATE));
            ctx.espace(16);

            ctx.sousTitre("Chiffres clés");
            ctx.ligneKpi("Moyenne générale",
                    qualite.moyenneGenerale() == null ? "aucun avis" : qualite.moyenneGenerale() + " / 5");
            ctx.ligneKpi("Nombre total d'avis déposés", String.valueOf(qualite.nombreAvis()));
            ctx.ligneKpi("Cours évalués", String.valueOf(qualite.coursEvalues()));
            ctx.ligneKpi("Cours à surveiller (moyenne <= 3 / 5)", String.valueOf(qualite.coursASurveiller()));
            ctx.espace(16);

            ctx.sousTitre("Détail par cours (du moins bien noté au mieux noté)");
            if (qualite.parCours().isEmpty()) {
                ctx.texte(ctx.italique, 10, "Aucun avis n'a encore été déposé.");
            } else {
                for (QualiteCoursDto.LigneQualite ligne : qualite.parCours()) {
                    String moyenne = ligne.moyenne() == null ? "-" : ligne.moyenne() + " / 5";
                    String entete = (ligne.coursCode() == null || ligne.coursCode().isBlank()
                            ? "" : ligne.coursCode() + " - ") + ligne.coursTitre();
                    ctx.ligneListe(entete);
                    ctx.ligneDetail("Moyenne " + moyenne
                            + "  |  " + ligne.nombreAvis() + " avis"
                            + "  |  " + ligne.actionsRequises() + " avis <= 3 / 5");
                    for (String commentaire : ligne.derniersCommentaires()) {
                        ctx.ligneDetail("« " + commentaire + " »");
                    }
                    ctx.espace(4);
                }
            }
            ctx.espace(12);

            ctx.sousTitre("Cours à surveiller");
            List<QualiteCoursDto.LigneQualite> aSurveiller = qualite.parCours().stream()
                    .filter(l -> l.moyenne() != null && l.moyenne() <= 3)
                    .toList();
            if (aSurveiller.isEmpty()) {
                ctx.texte(ctx.italique, 10, "Aucun cours sous le seuil d'alerte. Rien ne requiert d'action immédiate.");
            } else {
                ctx.texte(ctx.normal, 10,
                        "Ces cours ont une moyenne d'au plus 3 / 5 : ils appellent un échange avec l'équipe pédagogique.");
                for (QualiteCoursDto.LigneQualite ligne : aSurveiller) {
                    ctx.ligneListe((ligne.coursCode() == null ? "" : ligne.coursCode() + " - ") + ligne.coursTitre()
                            + " : " + ligne.moyenne() + " / 5 (" + ligne.nombreAvis() + " avis)");
                    for (String commentaire : ligne.derniersCommentaires()) {
                        ctx.ligneDetail("« " + commentaire + " »");
                    }
                }
            }

            ctx.fermer();
            return octets(document);
        } catch (Exception exception) {
            throw new RuntimeException("La generation du rapport sur les avis a echoue.", exception);
        }
    }

    // ======================================================================
    // Rapport 2 — Enquêtes de suivi de carrière
    // ======================================================================

    public byte[] genererRapportEnquetes(List<SyntheseEnqueteSuiviDto> enquetes) {
        try (PDDocument document = new PDDocument()) {
            Contexte ctx = new Contexte(document);

            ctx.titre("Rapport qualité — Enquêtes de suivi de carrière");
            ctx.texte(ctx.italique, 10, "Institut National du Pétrole et du Gaz");
            ctx.texte(ctx.italique, 10, "Généré le " + LocalDateTime.now().format(FORMAT_DATE));
            ctx.espace(16);

            int totalReponses = enquetes.stream().mapToInt(SyntheseEnqueteSuiviDto::nombreReponses).sum();
            long actives = enquetes.stream().filter(SyntheseEnqueteSuiviDto::active).count();
            ctx.sousTitre("Chiffres clés");
            ctx.ligneKpi("Campagnes lancées", String.valueOf(enquetes.size()));
            ctx.ligneKpi("Campagnes actives", String.valueOf(actives));
            ctx.ligneKpi("Total des réponses recueillies", String.valueOf(totalReponses));
            ctx.espace(16);

            ctx.sousTitre("Détail par campagne");
            if (enquetes.isEmpty()) {
                ctx.texte(ctx.italique, 10, "Aucune campagne d'enquête n'a encore été lancée auprès des alumni.");
            } else {
                for (SyntheseEnqueteSuiviDto enquete : enquetes) {
                    ctx.ligneListe(enquete.titre() + "  (" + (enquete.active() ? "active" : "clôturée") + ")");
                    if (enquete.description() != null && !enquete.description().isBlank()) {
                        ctx.ligneDetail(enquete.description());
                    }
                    if (enquete.dateCreation() != null) {
                        ctx.ligneDetail("Lancée le " + enquete.dateCreation().format(FORMAT_DATE));
                    }
                    String taux = tauxReponse(enquete);
                    ctx.ligneDetail("Réponses : " + enquete.nombreReponses()
                            + (enquete.nombreDestinataires() > 0
                                    ? " / " + enquete.nombreDestinataires() + " alumni (" + taux + ")"
                                    : ""));
                    ctx.ligneDetail("Application des compétences : "
                            + (enquete.moyenneApplicationCompetences() == null ? "-"
                                    : enquete.moyenneApplicationCompetences() + " / 5")
                            + "   |   Impact sur la carrière : "
                            + (enquete.moyenneImpactCarriere() == null ? "-"
                                    : enquete.moyenneImpactCarriere() + " / 5"));
                    for (String commentaire : enquete.derniersCommentaires()) {
                        ctx.ligneDetail("« " + commentaire + " »");
                    }
                    ctx.espace(6);
                }
            }

            ctx.fermer();
            return octets(document);
        } catch (Exception exception) {
            throw new RuntimeException("La generation du rapport sur les enquetes a echoue.", exception);
        }
    }

    private String tauxReponse(SyntheseEnqueteSuiviDto enquete) {
        if (enquete.nombreDestinataires() <= 0) {
            return "-";
        }
        return Math.round(100.0 * enquete.nombreReponses() / enquete.nombreDestinataires()) + " %";
    }

    private byte[] octets(PDDocument document) throws IOException {
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        document.save(sortie);
        return sortie.toByteArray();
    }

    /**
     * Rend un texte sûr pour l'encodage WinAnsi de Helvetica : tout caractère
     * hors Latin-1 (émoji, alphabets non latins glissés dans un titre de cours
     * ou d'enquête saisi librement) est remplacé par '?', ce qui evite un
     * {@code IllegalArgumentException} en pleine generation.
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

        /** Coupe le texte plutot que de le laisser deborder de la page. */
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
