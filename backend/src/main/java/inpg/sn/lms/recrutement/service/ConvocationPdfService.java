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
import inpg.sn.lms.recrutement.entity.Entretien;

@Service
public class ConvocationPdfService {

    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy 'à' HH:mm", Locale.FRENCH);
    private static final String VIDE = "—";

    private static final java.awt.Color BLEU_INPG = new java.awt.Color(0x30, 0x60, 0x9a);
    private static final java.awt.Color BLEU_FONCE = new java.awt.Color(0x1a, 0x36, 0x5d);
    private static final java.awt.Color GRIS_TEXTE = new java.awt.Color(0x46, 0x46, 0x4e);
    private static final java.awt.Color GRIS_FOND = new java.awt.Color(0xf2, 0xf4, 0xf6);
    private static final java.awt.Color GRIS_BORDURE = new java.awt.Color(0xc7, 0xc5, 0xd0);

    public byte[] genererConvocation(Entretien entretien, Candidature candidature) {
        Candidat candidat = entretien.getCandidat();

        Document document = new Document(PageSize.A4, 45, 45, 45, 45);
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, sortie);
            document.open();

            document.add(enTete());
            document.add(titre(candidature));
            document.add(tableauDetails(entretien, candidature, candidat));
            document.add(checklist());
            document.add(signatureEtCachet());

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Impossible de générer la convocation à l'entretien", e);
        }
        return sortie.toByteArray();
    }

    private Element enTete() {
        Paragraph marque = new Paragraph("INPG LMS", police(20, Font.BOLD, BLEU_INPG));
        marque.setSpacingAfter(2f);

        Paragraph institut = new Paragraph("Institut National du Pétrole et du Gaz\nDirection des Admissions et Concours",
                police(10, Font.NORMAL, GRIS_TEXTE));
        institut.setSpacingAfter(20f);

        Paragraph bloc = new Paragraph();
        bloc.add(marque);
        bloc.add(institut);
        return bloc;
    }

    private Element titre(Candidature candidature) {
        Paragraph titre = new Paragraph("CONVOCATION OFFICIELLE", police(16, Font.BOLD, BLEU_FONCE));
        titre.setSpacingAfter(4f);

        String session = candidature != null && candidature.getSessionCandidature() != null
                ? candidature.getSessionCandidature().getLibelle()
                : "Recrutement & Admissions";

        Paragraph sousTitre = new Paragraph("Entretien d'Admission — " + session, police(12, Font.NORMAL, GRIS_TEXTE));
        sousTitre.setSpacingAfter(18f);

        Paragraph bloc = new Paragraph();
        bloc.add(titre);
        bloc.add(sousTitre);
        return bloc;
    }

    private Element tableauDetails(Entretien entretien, Candidature candidature, Candidat candidat) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingAfter(18f);

        String numeroDossier = candidature != null ? nvl(candidature.getNumeroDossier()) : VIDE;
        String nomComplet = candidat != null
                ? (nvl(candidat.getPrenom()) + " " + nvl(candidat.getNom())).trim()
                : VIDE;
        String email = candidat != null && candidat.getUser() != null
                ? nvl(candidat.getUser().getEmail())
                : VIDE;
        String telephone = candidat != null ? nvl(candidat.getTelephone()) : VIDE;

        String dateHeureStr = entretien.getDateHeure() != null
                ? entretien.getDateHeure().format(DATETIME_FORMAT) + " GMT"
                : VIDE;
        String lieuOuLien = nvl(entretien.getLieuOuLien());

        ajouterLigne(table, "Numéro de dossier", numeroDossier);
        ajouterLigne(table, "Candidat(e)", nomComplet);
        ajouterLigne(table, "Email / Téléphone", email + " | " + telephone);
        ajouterLigne(table, "Date & Heure du rendez-vous", dateHeureStr);
        ajouterLigne(table, "Lieu / Lien de connexion", lieuOuLien);
        ajouterLigne(table, "Durée estimée", "30 minutes (Jury d'admission)");

        return table;
    }

    private Element checklist() {
        Paragraph titreSection = new Paragraph("Consignes et documents requis pour le jour J :",
                police(11, Font.BOLD, BLEU_FONCE));
        titreSection.setSpacingAfter(8f);

        Paragraph p1 = new Paragraph("• Présenter une pièce d'identité originale en cours de validité (CNI ou Passeport).",
                police(10, Font.NORMAL, GRIS_TEXTE));
        Paragraph p2 = new Paragraph("• Se munir des originaux des diplômes et relevés de notes téléchargés lors de la candidature.",
                police(10, Font.NORMAL, GRIS_TEXTE));
        Paragraph p3 = new Paragraph("• Présenter cette fiche de convocation officielle imprimée ou sur smartphone.",
                police(10, Font.NORMAL, GRIS_TEXTE));
        Paragraph p4 = new Paragraph("• Se présenter ou se connecter au moins 15 minutes avant l'heure fixée.",
                police(10, Font.NORMAL, GRIS_TEXTE));
        p4.setSpacingAfter(20f);

        Paragraph bloc = new Paragraph();
        bloc.add(titreSection);
        bloc.add(p1);
        bloc.add(p2);
        bloc.add(p3);
        bloc.add(p4);
        return bloc;
    }

    private Element signatureEtCachet() {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);

        PdfPCell c1 = new PdfPCell(new Paragraph("Dakar, le " + java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), police(9, Font.ITALIC, GRIS_TEXTE)));
        c1.setBorder(0);

        PdfPCell c2 = new PdfPCell(new Paragraph("Pour la Direction des Admissions\nCommission de Recrutement INPG", police(10, Font.BOLD, BLEU_FONCE)));
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c2.setBorder(0);

        table.addCell(c1);
        table.addCell(c2);
        return table;
    }

    private void ajouterLigne(PdfPTable table, String libelle, String valeur) {
        PdfPCell etiquette = new PdfPCell(new Phrase(libelle, police(10, Font.BOLD, BLEU_FONCE)));
        etiquette.setBackgroundColor(GRIS_FOND);
        etiquette.setBorderColor(GRIS_BORDURE);
        etiquette.setPadding(8f);

        PdfPCell contenu = new PdfPCell(new Phrase(valeur, police(10, Font.NORMAL, GRIS_TEXTE)));
        contenu.setBorderColor(GRIS_BORDURE);
        contenu.setPadding(8f);

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
