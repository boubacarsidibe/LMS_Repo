package inpg.sn.lms.personnel.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
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
import inpg.sn.lms.personnel.enums.EnumTypeActivite;

/**
 * Rapports d'activités du personnel, en PDF (TDR §I : « génération automatique
 * des rapports mensuels par département » et « compilation des rapports pour
 * un rapport global de l'INPG »).
 *
 * <p>Aucun recalcul métier : le service met en page les {@link ActivitePersonnel}
 * déjà filtrées pour la période, en les regroupant par type et en résumant en
 * quelques indicateurs (nombre d'activités, de contributeurs, répartition).
 */
@Service
public class RapportActivitePdfService {

    private static final float MARGE = 50f;
    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] MOIS_LIBELLES = {
            "Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
            "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"
    };
    /** Encodage réellement utilisé par Helvetica dans PDFBox. */
    private static final Charset WINANSI = Charset.forName("windows-1252");

    /** Rapport d'un seul département, pour la période donnée. */
    public byte[] genererRapportDepartement(String libelleDepartement, int mois, int annee,
            List<ActivitePersonnel> activites) {
        try (PDDocument document = new PDDocument()) {
            Contexte ctx = new Contexte(document);
            ctx.titre("Rapport d'activités — " + libelleDepartement);
            enTete(ctx, mois, annee);
            ctx.espace(10);
            indicateurs(ctx, activites);
            ctx.espace(10);
            sectionParType(ctx, activites);
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

            ctx.sousTitre("Synthèse de l'établissement");
            long departements = activites.stream().map(a -> a.getDepartement().getId()).distinct().count();
            ctx.ligneKpi("Départements ayant saisi une activité", String.valueOf(departements));
            ctx.ligneKpi("Total des activités", String.valueOf(activites.size()));
            ctx.ligneKpi("Contributeurs distincts", String.valueOf(nbContributeurs(activites)));
            repartitionParType(ctx, activites);
            ctx.espace(12);

            Map<String, List<ActivitePersonnel>> parDepartement = activites.stream()
                    .collect(Collectors.groupingBy(a -> a.getDepartement().getLibelle(), Collectors.toList()));
            parDepartement.keySet().stream().sorted().forEach(libelle -> {
                try {
                    ctx.sousTitre(libelle);
                    indicateurs(ctx, parDepartement.get(libelle));
                    ctx.espace(4);
                    sectionParType(ctx, parDepartement.get(libelle));
                    ctx.espace(8);
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

    /** Bloc « chiffres clés » d'un lot d'activités. */
    private void indicateurs(Contexte ctx, List<ActivitePersonnel> activites) throws IOException {
        ctx.ligneKpi("Activités saisies", String.valueOf(activites.size()));
        ctx.ligneKpi("Contributeurs distincts", String.valueOf(nbContributeurs(activites)));
        repartitionParType(ctx, activites);
    }

    private long nbContributeurs(List<ActivitePersonnel> activites) {
        return activites.stream()
                .map(a -> a.getAuteur() == null ? null : a.getAuteur().getId())
                .filter(java.util.Objects::nonNull)
                .distinct()
                .count();
    }

    private void repartitionParType(Contexte ctx, List<ActivitePersonnel> activites) throws IOException {
        if (activites.isEmpty()) {
            return;
        }
        Map<EnumTypeActivite, Long> comptes = activites.stream()
                .collect(Collectors.groupingBy(ActivitePersonnel::getTypeActivite, Collectors.counting()));
        String detail = java.util.Arrays.stream(EnumTypeActivite.values())
                .filter(comptes::containsKey)
                .map(t -> libelleType(t.name()) + " : " + comptes.get(t))
                .collect(Collectors.joining("  |  "));
        ctx.ligneKpi("Répartition par type", detail);
    }

    /** Les activités regroupées par type, chacune datée avec sa description. */
    private void sectionParType(Contexte ctx, List<ActivitePersonnel> activites) throws IOException {
        if (activites.isEmpty()) {
            ctx.texte(ctx.italique, 10, "Aucune activité saisie pour cette période.");
            return;
        }
        Map<EnumTypeActivite, List<ActivitePersonnel>> parType = new LinkedHashMap<>();
        for (EnumTypeActivite type : EnumTypeActivite.values()) {
            List<ActivitePersonnel> duType = activites.stream()
                    .filter(a -> a.getTypeActivite() == type)
                    .sorted((a, b) -> b.getDateCreation().compareTo(a.getDateCreation()))
                    .toList();
            if (!duType.isEmpty()) {
                parType.put(type, duType);
            }
        }
        for (Map.Entry<EnumTypeActivite, List<ActivitePersonnel>> entree : parType.entrySet()) {
            ctx.petitTitre(libelleType(entree.getKey().name()) + " (" + entree.getValue().size() + ")");
            for (ActivitePersonnel activite : entree.getValue()) {
                String auteur = activite.getAuteur() == null ? "?" : activite.getAuteur().getUsername();
                ctx.ligneListe(activite.getDateCreation().format(FORMAT_DATE) + " — " + activite.getTitre()
                        + "  (" + auteur + ")");
                if (activite.getDescription() != null && !activite.getDescription().isBlank()) {
                    ctx.ligneDetail(activite.getDescription());
                }
            }
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

    /**
     * Rend un texte sûr pour l'encodage WinAnsi de Helvetica : tout caractère
     * hors Latin-1 (émoji, alphabet non latin dans un titre saisi librement) est
     * remplacé par '?' — évite un {@code IllegalArgumentException} en pleine
     * génération, que l'ancienne version laissait remonter en 500.
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

        void petitTitre(String texte) throws IOException {
            espace(4);
            assurerEspace(18);
            ecrire(gras, 11, texte);
        }

        void ligneKpi(String libelle, String valeur) throws IOException {
            assurerEspace(16);
            ecrire(normal, 11, libelle + " : " + valeur);
        }

        void ligneListe(String texte) throws IOException {
            assurerEspace(16);
            ecrireIndente(normal, 10, "- " + texte, 0f);
        }

        void ligneDetail(String texte) throws IOException {
            assurerEspace(14);
            ecrireIndente(italique, 9, texte, 12f);
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
