package inpg.sn.lms.personnel.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import inpg.sn.lms.personnel.entity.ActivitePersonnel;

/**
 * Rapports d'activités du personnel, en PDF (TDR §I : « génération automatique
 * des rapports mensuels par département » et « compilation des rapports pour
 * un rapport global de l'INPG »).
 *
 * <p>Même bibliothèque et même patron que {@code RapportQualitePdfService} :
 * mise en page simple d'un rapport interne, sans rien recalculer au-delà du
 * regroupement par département déjà nécessaire à l'affichage.
 */
@Service
public class RapportActivitePdfService {

    private static final float MARGE = 50f;
    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] MOIS_LIBELLES = {
            "Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
            "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"
    };

    /** Rapport d'un seul département, pour la période donnée. */
    public byte[] genererRapportDepartement(String libelleDepartement, int mois, int annee,
            List<ActivitePersonnel> activites) {
        try (PDDocument document = new PDDocument()) {
            Contexte ctx = new Contexte(document);
            ctx.titre("Rapport d'activités — " + libelleDepartement);
            enTete(ctx, mois, annee);
            ctx.espace(10);
            sectionActivites(ctx, activites);
            ctx.fermer();
            return octets(document);
        } catch (Exception exception) {
            throw new RuntimeException("La génération du rapport d'activités a échoué.", exception);
        }
    }

    /** Rapport consolidé de tous les départements, pour la période donnée. */
    public byte[] genererRapportGlobal(int mois, int annee, List<ActivitePersonnel> activites) {
        try (PDDocument document = new PDDocument()) {
            Contexte ctx = new Contexte(document);
            ctx.titre("Rapport global d'activités — INPG");
            enTete(ctx, mois, annee);
            ctx.espace(10);
            ctx.ligneKpi("Départements ayant saisi une activité",
                    String.valueOf(activites.stream().map(a -> a.getDepartement().getId()).distinct().count()));
            ctx.ligneKpi("Total des activités", String.valueOf(activites.size()));
            ctx.espace(10);

            Map<String, List<ActivitePersonnel>> parDepartement = activites.stream()
                    .collect(Collectors.groupingBy(a -> a.getDepartement().getLibelle(), Collectors.toList()));
            parDepartement.keySet().stream().sorted().forEach(libelle -> {
                try {
                    ctx.sousTitre(libelle);
                    sectionActivites(ctx, parDepartement.get(libelle));
                    ctx.espace(6);
                } catch (IOException exception) {
                    throw new RuntimeException(exception);
                }
            });
            ctx.fermer();
            return octets(document);
        } catch (Exception exception) {
            throw new RuntimeException("La génération du rapport global a échoué.", exception);
        }
    }

    private void enTete(Contexte ctx, int mois, int annee) throws IOException {
        ctx.texte(ctx.italique, 10,
                "Période : " + MOIS_LIBELLES[mois - 1] + " " + annee
                        + " — généré le " + LocalDateTime.now().format(FORMAT_DATE));
    }

    private void sectionActivites(Contexte ctx, List<ActivitePersonnel> activites) throws IOException {
        if (activites.isEmpty()) {
            ctx.texte(ctx.italique, 10, "Aucune activité saisie pour cette période.");
            return;
        }
        for (ActivitePersonnel activite : activites) {
            String auteur = activite.getAuteur() == null ? "?" : activite.getAuteur().getUsername();
            ctx.ligneListe("[" + libelleType(activite.getTypeActivite().name()) + "] " + activite.getTitre()
                    + " — " + auteur);
        }
    }

    private String libelleType(String type) {
        return type.charAt(0) + type.substring(1).toLowerCase(Locale.FRENCH);
    }

    private byte[] octets(PDDocument document) throws IOException {
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        document.save(sortie);
        return sortie.toByteArray();
    }

    /** Petite machine à écrire verticale avec saut de page automatique — identique à RapportQualitePdfService. */
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
            ecrire(gras, 18, texte);
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
            ecrire(gras, 13, texte);
        }

        void ligneKpi(String libelle, String valeur) throws IOException {
            assurerEspace(16);
            ecrire(normal, 11, libelle + " : " + valeur);
        }

        void ligneListe(String texte) throws IOException {
            assurerEspace(16);
            ecrire(normal, 10, "- " + texte);
        }

        void texte(PDType1Font font, float taille, String texte) throws IOException {
            assurerEspace(16);
            ecrire(font, taille, texte);
        }

        void espace(float hauteur) {
            y -= hauteur;
        }

        private void ecrire(PDType1Font font, float taille, String texte) throws IOException {
            content.beginText();
            content.setFont(font, taille);
            content.newLineAtOffset(MARGE, y);
            content.showText(tronquer(texte, font, taille, largeurUtile));
            content.endText();
            y -= taille + 6;
        }

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
