package inpg.sn.lms.recrutement.service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
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

import inpg.sn.lms.auth.entity.Candidat;
import inpg.sn.lms.recrutement.entity.Candidature;

@Service
public class LettreAdmissionPdfService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH);
    private static final String VIDE = "—";

    private static final java.awt.Color BLEU_INPG = new java.awt.Color(0x30, 0x60, 0x9a);
    private static final java.awt.Color BLEU_FONCE = new java.awt.Color(0x1a, 0x36, 0x5d);
    private static final java.awt.Color VERT_SUCCES = new java.awt.Color(0x05, 0x96, 0x69);
    private static final java.awt.Color GRIS_TEXTE = new java.awt.Color(0x46, 0x46, 0x4e);
    private static final java.awt.Color GRIS_FOND = new java.awt.Color(0xf8, 0xfa, 0xfc);
    private static final java.awt.Color GRIS_BORDURE = new java.awt.Color(0xc7, 0xc5, 0xd0);

    public byte[] genererLettreAdmission(Candidature candidature) {
        Candidat candidat = candidature.getCandidat();

        Document document = new Document(PageSize.A4, 45, 45, 45, 45);
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, sortie);
            document.open();

            document.add(enTete());
            document.add(titre(candidature));
            document.add(corpsLettre(candidature, candidat));
            document.add(tableauRecapitulatif(candidature, candidat));
            document.add(prochainesEtapes());
            document.add(signatureEtCachet());

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Impossible de générer la lettre d'admission", e);
        }
        return sortie.toByteArray();
    }

    private Element enTete() {
        Paragraph marque = new Paragraph("INPG LMS", police(22, Font.BOLD, BLEU_INPG));
        marque.setSpacingAfter(2f);

        Paragraph institut = new Paragraph("Institut National Polytechnique de Gestion\nDirection Générale — Service des Admissions et de la Scolarité",
                police(10, Font.NORMAL, GRIS_TEXTE));
        institut.setSpacingAfter(24f);

        Paragraph bloc = new Paragraph();
        bloc.add(marque);
        bloc.add(institut);
        return bloc;
    }

    private Element titre(Candidature candidature) {
        Paragraph titre = new Paragraph("NOTIFICATION OFFICIELLE D'ADMISSION", police(16, Font.BOLD, BLEU_FONCE));
        titre.setAlignment(Element.ALIGN_CENTER);
        titre.setSpacingAfter(4f);

        Paragraph statutBadge = new Paragraph("DÉCISION : ADMIS DÉFINITIF", police(12, Font.BOLD, VERT_SUCCES));
        statutBadge.setAlignment(Element.ALIGN_CENTER);
        statutBadge.setSpacingAfter(20f);

        Paragraph bloc = new Paragraph();
        bloc.add(titre);
        bloc.add(statutBadge);
        return bloc;
    }

    private Element corpsLettre(Candidature candidature, Candidat candidat) {
        String nomComplet = candidat != null ? (nvl(candidat.getPrenom()) + " " + nvl(candidat.getNom())).trim() : "Candidat(e)";
        String session = candidature.getSessionCandidature() != null ? candidature.getSessionCandidature().getLibelle() : "Formation INPG";

        Paragraph salutation = new Paragraph("Chère / Cher " + nomComplet + ",", police(11, Font.BOLD, BLEU_FONCE));
        salutation.setSpacingAfter(8f);

        Paragraph texte = new Paragraph(
                "Nous avons le plaisir et l'honneur de vous informer qu'à l'issue des délibérations du jury d'admission, votre dossier de candidature a été officiellement retenu pour intégrer le programme :",
                police(10, Font.NORMAL, GRIS_TEXTE));
        texte.setSpacingAfter(6f);

        Paragraph prog = new Paragraph("« " + session + " »", police(12, Font.BOLD, BLEU_INPG));
        prog.setAlignment(Element.ALIGN_CENTER);
        prog.setSpacingAfter(10f);

        Paragraph suite = new Paragraph(
                "Toute la communauté pédagogique et administrative de l'Institut National Polytechnique de Gestion (INPG) vous adresse ses plus vives félicitations pour la qualité de votre parcours et votre réussite.",
                police(10, Font.NORMAL, GRIS_TEXTE));
        suite.setSpacingAfter(16f);

        Paragraph bloc = new Paragraph();
        bloc.add(salutation);
        bloc.add(texte);
        bloc.add(prog);
        bloc.add(suite);
        return bloc;
    }

    private Element tableauRecapitulatif(Candidature candidature, Candidat candidat) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingAfter(18f);

        String dossier = nvl(candidature.getNumeroDossier());
        String dateSoumission = candidature.getDateSoumission() != null
                ? candidature.getDateSoumission().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                : VIDE;
        String dateDecision = java.time.LocalDate.now().format(DATE_FORMAT);

        ajouterLigne(table, "N° de Dossier", dossier);
        ajouterLigne(table, "Date de soumission", dateSoumission);
        ajouterLigne(table, "Date de décision officielle", dateDecision);
        ajouterLigne(table, "Statut de scolarité", "Inscrit / Étudiant INPG");

        return table;
    }

    private Element prochainesEtapes() {
        Paragraph titreSection = new Paragraph("Prochaines étapes pour finaliser votre rentrée :",
                police(11, Font.BOLD, BLEU_FONCE));
        titreSection.setSpacingAfter(6f);

        Paragraph e1 = new Paragraph("1. Connexion à votre Espace Étudiant LMS avec vos identifiants actuels.", police(9.5f, Font.NORMAL, GRIS_TEXTE));
        Paragraph e2 = new Paragraph("2. Règlement des frais de scolarité et transmission du dossier physique.", police(9.5f, Font.NORMAL, GRIS_TEXTE));
        Paragraph e3 = new Paragraph("3. Retrait de votre carte d'étudiant lors de la journée d'intégration.", police(9.5f, Font.NORMAL, GRIS_TEXTE));
        e3.setSpacingAfter(20f);

        Paragraph bloc = new Paragraph();
        bloc.add(titreSection);
        bloc.add(e1);
        bloc.add(e2);
        bloc.add(e3);
        return bloc;
    }

    private Element signatureEtCachet() {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);

        PdfPCell c1 = new PdfPCell(new Paragraph("Dakar, le " + java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), police(9, Font.ITALIC, GRIS_TEXTE)));
        c1.setBorder(0);

        PdfPCell c2 = new PdfPCell(new Paragraph("Le Directeur des Admissions & Concours\nINPG Sénégal", police(10, Font.BOLD, BLEU_FONCE)));
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c2.setBorder(0);

        table.addCell(c1);
        table.addCell(c2);
        return table;
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

    private Font police(float taille, int style, java.awt.Color couleur) {
        return FontFactory.getFont(FontFactory.HELVETICA, taille, style, couleur);
    }

    private String nvl(String val) {
        return (val == null || val.isBlank()) ? VIDE : val;
    }
}
