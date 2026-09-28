package inpg.sn.lms.entreprise.service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import inpg.sn.lms.auth.entity.Entreprise;
import inpg.sn.lms.entreprise.entity.DemandeEntreprise;
import inpg.sn.lms.entreprise.entity.FicheModuleJour;
import inpg.sn.lms.entreprise.entity.FicheTechniqueModule;
import inpg.sn.lms.pdf.PapierEnTete;
import inpg.sn.lms.pdf.PapierEnTeteOpenPdf;

/**
 * Deux documents téléchargeables dérivés d'une même {@link FicheTechniqueModule} acceptée (TDR §H) :
 * un devis (proposition commerciale courte, pour la comptabilité/les achats de l'entreprise) et la
 * convention de formation complète (les dix sections du gabarit + planning, pour le dossier RH/formation).
 * Aucun modèle officiel de l'INPG n'existe pour ces deux documents — contrairement au certificat de
 * formation, ils sont entièrement mis en page par l'application sur le papier à en-tête commun, comme
 * l'attestation d'accréditation et la lettre d'admission.
 */
@Service
public class FicheTechniqueModulePdfService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH);
    private static final String VIDE = "—";

    private static final java.awt.Color BLEU_INPG = new java.awt.Color(0x30, 0x60, 0x9a);
    private static final java.awt.Color BLEU_FONCE = new java.awt.Color(0x1a, 0x36, 0x5d);
    private static final java.awt.Color GRIS_TEXTE = new java.awt.Color(0x46, 0x46, 0x4e);
    private static final java.awt.Color GRIS_FOND = new java.awt.Color(0xf8, 0xfa, 0xfc);
    private static final java.awt.Color GRIS_BORDURE = new java.awt.Color(0xc7, 0xc5, 0xd0);

    // ===================== Devis =====================

    public byte[] genererDevisPdf(FicheTechniqueModule fiche) {
        Document document = nouveauDocument();
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, sortie).setPageEvent(new PapierEnTeteOpenPdf());
            document.open();

            document.add(entete("Direction de la Régulation et du Contrôle Qualité (DRCQ)"));
            PapierEnTeteOpenPdf.ajouter(document, titre("PROPOSITION COMMERCIALE",
                    "Devis pour une formation sur mesure"));
            PapierEnTeteOpenPdf.ajouter(document, introDevis(fiche));
            document.add(tableauDevis(fiche));
            document.add(signature(fiche, "Pour l'INPG", "La Direction de la Régulation et du Contrôle Qualité (DRCQ)"));

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Impossible de générer le devis.", e);
        }
        return sortie.toByteArray();
    }

    private List<Element> introDevis(FicheTechniqueModule fiche) {
        String raisonSociale = raisonSociale(fiche);
        Paragraph texte = new Paragraph(
                "À l'attention de " + raisonSociale + ", l'Institut National du Pétrole et du Gaz (INPG) propose "
                        + "la formation sur mesure suivante :",
                police(10, Font.NORMAL, GRIS_TEXTE));
        texte.setSpacingAfter(10f);

        Paragraph intitule = new Paragraph("« " + nvl(fiche.getIntitule()) + " »", police(13, Font.BOLD, BLEU_INPG));
        intitule.setAlignment(Element.ALIGN_CENTER);
        intitule.setSpacingAfter(16f);

        return List.of(texte, intitule);
    }

    private Element tableauDevis(FicheTechniqueModule fiche) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingAfter(16f);

        ajouterLigneSiPresente(table, "Niveau", fiche.getNiveau());
        ajouterLigneSiPresente(table, "Effectif", fiche.getEffectif() == null ? null : fiche.getEffectif() + " personne(s)");
        String duree = libelleDuree(fiche);
        ajouterLigneSiPresente(table, "Durée", duree);
        ajouterLigneSiPresente(table, "Période probable", fiche.getPeriodeProbable());
        ajouterLigneSiPresente(table, "Objectif général", fiche.getObjectifGeneral());
        ajouterLigne(table, "Coût total", nvl(fiche.getCoutTotal()));
        ajouterLigneSiPresente(table, "Modalités financières", fiche.getModalitesFinancieres());

        return table;
    }

    // ===================== Convention =====================

    public byte[] genererConventionPdf(FicheTechniqueModule fiche) {
        Document document = nouveauDocument();
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, sortie).setPageEvent(new PapierEnTeteOpenPdf());
            document.open();

            document.add(entete(referenceEnTete(fiche)));
            PapierEnTeteOpenPdf.ajouter(document, titre("CONVENTION DE FORMATION",
                    "Entre l'INPG et " + raisonSociale(fiche)));
            PapierEnTeteOpenPdf.ajouter(document, List.of(intituleFormation(fiche)));
            document.add(tableauCibles(fiche));
            PapierEnTeteOpenPdf.ajouter(document, sectionTexte("Objectif général", fiche.getObjectifGeneral()));
            PapierEnTeteOpenPdf.ajouter(document, sectionTexte("Objectifs spécifiques", fiche.getObjectifsSpecifiques()));
            PapierEnTeteOpenPdf.ajouter(document, sectionTexte("Modalités de test des prérequis", fiche.getModalitesTestPrerequis()));
            PapierEnTeteOpenPdf.ajouter(document, sectionTexte("Formation sur site", fiche.getFormationSurSite()));
            PapierEnTeteOpenPdf.ajouter(document, sectionTexte("Types d'évaluation", fiche.getTypesEvaluation()));
            PapierEnTeteOpenPdf.ajouter(document, sectionTexte("Ressources pédagogiques", fiche.getRessourcesPedagogiques()));
            PapierEnTeteOpenPdf.ajouter(document, sectionTexte("Bibliographie", fiche.getBibliographie()));
            PapierEnTeteOpenPdf.ajouter(document, sectionTexte("Profil des formateurs", fiche.getProfilFormateur()));
            if (!fiche.getJours().isEmpty()) {
                PapierEnTeteOpenPdf.ajouter(document, List.of(titreSection("Planning")));
                document.add(tableauPlanning(fiche.getJours()));
            }
            if (nonVide(fiche.getCoutTotal()) || nonVide(fiche.getModalitesFinancieres())) {
                document.add(tableauFinancier(fiche));
            }
            document.add(signature(fiche, "Pour l'INPG", nvl(fiche.getResponsableModule())));

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Impossible de générer la convention de formation.", e);
        }
        return sortie.toByteArray();
    }

    private Element tableauCibles(FicheTechniqueModule fiche) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingAfter(14f);

        ajouterLigneSiPresente(table, "Entreprise", raisonSociale(fiche));
        ajouterLigneSiPresente(table, "Département", fiche.getDepartement());
        ajouterLigneSiPresente(table, "Atelier / service", fiche.getAtelierService());
        ajouterLigneSiPresente(table, "Fonctions cibles", fiche.getFonctionsCibles());
        ajouterLigneSiPresente(table, "Thématiques", fiche.getThematiques());
        ajouterLigneSiPresente(table, "Prérequis", fiche.getPrerequis());
        ajouterLigne(table, "Langue", nvl(fiche.getLangue()));
        ajouterLigneSiPresente(table, "Niveau", fiche.getNiveau());
        ajouterLigneSiPresente(table, "Effectif", fiche.getEffectif() == null ? null : fiche.getEffectif() + " personne(s)");
        ajouterLigneSiPresente(table, "Durée", libelleDuree(fiche));
        ajouterLigneSiPresente(table, "Période probable", fiche.getPeriodeProbable());

        return table;
    }

    private Element tableauFinancier(FicheTechniqueModule fiche) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(4f);
        table.setSpacingAfter(16f);

        ajouterLigneSiPresente(table, "Coût total", fiche.getCoutTotal());
        ajouterLigneSiPresente(table, "Modalités financières", fiche.getModalitesFinancieres());

        return table;
    }

    private Element tableauPlanning(List<FicheModuleJour> jours) {
        PdfPTable table = new PdfPTable(new float[] { 1f, 4f, 1.5f, 2f, 2.5f });
        table.setWidthPercentage(100);
        table.setSpacingAfter(16f);

        for (String entete : new String[] { "Jour", "Contenus", "Durée", "Type", "Modalités" }) {
            PdfPCell cellule = new PdfPCell(new Phrase(entete, police(8.5f, Font.BOLD, java.awt.Color.WHITE)));
            cellule.setBackgroundColor(BLEU_INPG);
            cellule.setPadding(5f);
            table.addCell(cellule);
        }
        for (FicheModuleJour jour : jours) {
            table.addCell(cellule(String.valueOf(jour.getNumero())));
            table.addCell(cellule(nvl(jour.getContenus())));
            table.addCell(cellule(jour.getDureeHeures() == null ? VIDE : jour.getDureeHeures() + " h"));
            table.addCell(cellule(nvl(jour.getTypeEnseignement())));
            table.addCell(cellule(nvl(jour.getModalites())));
        }
        return table;
    }

    private PdfPCell cellule(String texte) {
        PdfPCell cellule = new PdfPCell(new Phrase(texte, police(8.5f, Font.NORMAL, GRIS_TEXTE)));
        cellule.setBorderColor(GRIS_BORDURE);
        cellule.setPadding(5f);
        return cellule;
    }

    // ===================== Commun =====================

    private Document nouveauDocument() {
        return new Document(PageSize.A4, PapierEnTete.MARGE_GAUCHE, PapierEnTete.MARGE_DROITE,
                PapierEnTete.HAUT_CONTENU, PapierEnTete.BAS_CONTENU);
    }

    private Element entete(String texte) {
        Paragraph p = new Paragraph(texte, police(10, Font.NORMAL, GRIS_TEXTE));
        p.setSpacingBefore(6f);
        p.setSpacingAfter(20f);
        return p;
    }

    private String referenceEnTete(FicheTechniqueModule fiche) {
        String base = "Direction de la Régulation et du Contrôle Qualité (DRCQ)";
        return nonVide(fiche.getCode()) ? base + " — Réf. " + fiche.getCode() : base;
    }

    private List<Element> titre(String titre, String sousTitre) {
        Paragraph t = new Paragraph(titre, police(16, Font.BOLD, BLEU_FONCE));
        t.setAlignment(Element.ALIGN_CENTER);
        t.setSpacingAfter(4f);

        Paragraph st = new Paragraph(sousTitre, police(11, Font.NORMAL, GRIS_TEXTE));
        st.setAlignment(Element.ALIGN_CENTER);
        st.setSpacingAfter(20f);

        return List.of(t, st);
    }

    private Paragraph intituleFormation(FicheTechniqueModule fiche) {
        Paragraph p = new Paragraph("« " + nvl(fiche.getIntitule()) + " »", police(13, Font.BOLD, BLEU_INPG));
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingAfter(14f);
        return p;
    }

    private Paragraph titreSection(String texte) {
        Paragraph p = new Paragraph(texte, police(11, Font.BOLD, BLEU_FONCE));
        p.setSpacingBefore(8f);
        p.setSpacingAfter(6f);
        return p;
    }

    /** Une section entière est omise si son texte n'a jamais été renseigné — comme côté écran (*ngIf). */
    private List<Element> sectionTexte(String titre, String contenu) {
        if (!nonVide(contenu)) {
            return List.of();
        }
        Paragraph corps = new Paragraph(contenu, police(9.5f, Font.NORMAL, GRIS_TEXTE));
        corps.setSpacingAfter(10f);
        return List.of(titreSection(titre), corps);
    }

    private Element signature(FicheTechniqueModule fiche, String libelleGauche, String signataire) {
        LocalDateTime date = fiche.getDateDecisionEntreprise() != null ? fiche.getDateDecisionEntreprise() : LocalDateTime.now();

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);

        PdfPCell gauche = new PdfPCell(new Paragraph("Dakar, le " + date.format(DATE_FORMAT), police(9, Font.ITALIC, GRIS_TEXTE)));
        gauche.setBorder(0);

        PdfPCell droite = new PdfPCell(new Paragraph(libelleGauche + "\n" + signataire, police(10, Font.BOLD, BLEU_FONCE)));
        droite.setHorizontalAlignment(Element.ALIGN_RIGHT);
        droite.setBorder(0);

        table.addCell(gauche);
        table.addCell(droite);
        return table;
    }

    private String libelleDuree(FicheTechniqueModule fiche) {
        if (fiche.getDureeJours() == null && fiche.getDureeHeures() == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (fiche.getDureeJours() != null) {
            sb.append(fiche.getDureeJours()).append(" j");
        }
        if (fiche.getDureeJours() != null && fiche.getDureeHeures() != null) {
            sb.append(" — ");
        }
        if (fiche.getDureeHeures() != null) {
            sb.append(fiche.getDureeHeures()).append(" h");
        }
        return sb.toString();
    }

    private String raisonSociale(FicheTechniqueModule fiche) {
        DemandeEntreprise demande = fiche.getDemande();
        Entreprise entreprise = demande != null ? demande.getEntreprise() : null;
        return entreprise != null && nonVide(entreprise.getRaisonSociale()) ? entreprise.getRaisonSociale() : VIDE;
    }

    private void ajouterLigne(PdfPTable table, String libelle, String valeur) {
        PdfPCell etiquette = new PdfPCell(new Phrase(libelle, police(9.5f, Font.BOLD, BLEU_FONCE)));
        etiquette.setBackgroundColor(GRIS_FOND);
        etiquette.setBorderColor(GRIS_BORDURE);
        etiquette.setPadding(6f);

        PdfPCell contenu = new PdfPCell(new Phrase(valeur, police(9.5f, Font.NORMAL, GRIS_TEXTE)));
        contenu.setBorderColor(GRIS_BORDURE);
        contenu.setPadding(6f);

        table.addCell(etiquette);
        table.addCell(contenu);
    }

    private void ajouterLigneSiPresente(PdfPTable table, String libelle, String valeur) {
        if (nonVide(valeur)) {
            ajouterLigne(table, libelle, valeur);
        }
    }

    private Font police(float taille, int style, java.awt.Color couleur) {
        return FontFactory.getFont(FontFactory.HELVETICA, taille, style, couleur);
    }

    private boolean nonVide(String valeur) {
        return valeur != null && !valeur.isBlank();
    }

    private String nvl(String val) {
        return nonVide(val) ? val : VIDE;
    }
}
