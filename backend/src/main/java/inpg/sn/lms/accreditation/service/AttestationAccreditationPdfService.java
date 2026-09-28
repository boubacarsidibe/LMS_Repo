package inpg.sn.lms.accreditation.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import inpg.sn.lms.accreditation.entity.DemandeAccreditation;
import inpg.sn.lms.accreditation.enums.EnumNiveauProgramme;
import inpg.sn.lms.accreditation.enums.EnumStatutDemandeAccreditation;
import inpg.sn.lms.accreditation.enums.EnumTypeStructure;
import inpg.sn.lms.pdf.PapierEnTete;
import inpg.sn.lms.pdf.PapierEnTeteOpenPdf;

/**
 * Attestation d'accréditation d'un programme de formation (TDR §F), au même
 * moteur que la lettre d'admission ({@link inpg.sn.lms.recrutement.service.LettreAdmissionPdfService}) :
 * papier à en-tête de l'INPG, document entièrement mis en page par l'application — contrairement au
 * certificat de formation, il n'existe aucun modèle officiel fourni par l'INPG pour ce document.
 *
 * <p>Le QR code pointe vers la vérification publique par référence (voir
 * {@code AttestationAccreditationPublicController}), sur le même principe que celui des certificats.
 */
@Service
public class AttestationAccreditationPdfService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH);
    private static final String VIDE = "—";

    private static final java.awt.Color BLEU_INPG = new java.awt.Color(0x30, 0x60, 0x9a);
    private static final java.awt.Color BLEU_FONCE = new java.awt.Color(0x1a, 0x36, 0x5d);
    private static final java.awt.Color VERT_SUCCES = new java.awt.Color(0x05, 0x96, 0x69);
    private static final java.awt.Color ORANGE_RESERVE = new java.awt.Color(0xb4, 0x5f, 0x06);
    private static final java.awt.Color GRIS_TEXTE = new java.awt.Color(0x46, 0x46, 0x4e);
    private static final java.awt.Color GRIS_FOND = new java.awt.Color(0xf8, 0xfa, 0xfc);
    private static final java.awt.Color GRIS_BORDURE = new java.awt.Color(0xc7, 0xc5, 0xd0);

    private static final Map<EnumNiveauProgramme, String> LIBELLES_NIVEAU = Map.of(
            EnumNiveauProgramme.TECHNICIEN, "Technicien",
            EnumNiveauProgramme.INGENIEUR, "Ingénieur",
            EnumNiveauProgramme.LICENCE, "Licence",
            EnumNiveauProgramme.MASTER, "Master",
            EnumNiveauProgramme.MASTERE_SPECIALISE, "Mastère spécialisé",
            EnumNiveauProgramme.CERTIFICAT, "Certificat / qualification",
            EnumNiveauProgramme.AUTRE, "Autre");

    private static final Map<EnumTypeStructure, String> LIBELLES_STRUCTURE = Map.of(
            EnumTypeStructure.UNIVERSITE_PUBLIQUE, "Université publique",
            EnumTypeStructure.ECOLE_INGENIEURS, "École d'ingénieurs",
            EnumTypeStructure.ECOLE_PRIVEE_RECONNUE, "École privée reconnue (référencée ANAQ-SUP)",
            EnumTypeStructure.CENTRE_TECHNIQUE_PRO, "Centre de formation technique et professionnel");

    private final String frontendUrl;

    public AttestationAccreditationPdfService(@Value("${app.frontend-url}") String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }

    public byte[] genererPdf(DemandeAccreditation demande) {
        boolean sousReserve = demande.getStatut() == EnumStatutDemandeAccreditation.ACCREDITE_SOUS_RESERVE;

        Document document = new Document(PageSize.A4, PapierEnTete.MARGE_GAUCHE, PapierEnTete.MARGE_DROITE,
                PapierEnTete.HAUT_CONTENU, PapierEnTete.BAS_CONTENU);
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, sortie).setPageEvent(new PapierEnTeteOpenPdf());
            document.open();

            document.add(enTete());
            PapierEnTeteOpenPdf.ajouter(document, titre(sousReserve));
            PapierEnTeteOpenPdf.ajouter(document, corps(demande, sousReserve));
            document.add(tableauRecapitulatif(demande, sousReserve));
            if (sousReserve && demande.getPlanActions() != null && !demande.getPlanActions().isBlank()) {
                PapierEnTeteOpenPdf.ajouter(document, planActions(demande.getPlanActions()));
            }
            document.add(signatureEtCachet(demande));
            if (demande.getReferenceAttestation() != null) {
                PapierEnTeteOpenPdf.ajouter(document, verificationQr(demande.getReferenceAttestation()));
            }

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Impossible de générer l'attestation d'accréditation", e);
        }
        return sortie.toByteArray();
    }

    private Element enTete() {
        Paragraph direction = new Paragraph("Direction de la Régulation et du Contrôle Qualité (DRCQ)",
                police(10, Font.NORMAL, GRIS_TEXTE));
        direction.setSpacingBefore(6f);
        direction.setSpacingAfter(20f);
        return direction;
    }

    private List<Element> titre(boolean sousReserve) {
        Paragraph titre = new Paragraph("ATTESTATION D'ACCRÉDITATION", police(16, Font.BOLD, BLEU_FONCE));
        titre.setAlignment(Element.ALIGN_CENTER);
        titre.setSpacingAfter(4f);

        Paragraph badge = new Paragraph(
                sousReserve ? "DÉCISION : ACCRÉDITÉ SOUS RÉSERVE" : "DÉCISION : ACCRÉDITÉ",
                police(12, Font.BOLD, sousReserve ? ORANGE_RESERVE : VERT_SUCCES));
        badge.setAlignment(Element.ALIGN_CENTER);
        badge.setSpacingAfter(20f);

        return List.of(titre, badge);
    }

    private List<Element> corps(DemandeAccreditation demande, boolean sousReserve) {
        String nomEcole = demande.getEcole() != null ? nvl(demande.getEcole().getNomEcole()) : VIDE;

        Paragraph texte = new Paragraph(
                "L'Institut National du Pétrole et du Gaz (INPG) atteste, à l'issue du processus d'évaluation "
                        + "prévu par le Cadre de référence DRCQ, que l'établissement ci-dessous est accrédité pour "
                        + "dispenser le programme de formation suivant, dans le domaine du pétrole et du gaz :",
                police(10, Font.NORMAL, GRIS_TEXTE));
        texte.setSpacingAfter(10f);

        Paragraph ecole = new Paragraph(nomEcole, police(13, Font.BOLD, BLEU_FONCE));
        ecole.setAlignment(Element.ALIGN_CENTER);
        ecole.setSpacingAfter(4f);

        Paragraph programme = new Paragraph("« " + nvl(demande.getIntituleProgramme()) + " »",
                police(12, Font.BOLD, BLEU_INPG));
        programme.setAlignment(Element.ALIGN_CENTER);
        programme.setSpacingAfter(16f);

        return List.of(texte, ecole, programme);
    }

    private Element tableauRecapitulatif(DemandeAccreditation demande, boolean sousReserve) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingAfter(14f);

        String niveau = demande.getNiveauProgramme() != null
                ? LIBELLES_NIVEAU.getOrDefault(demande.getNiveauProgramme(), demande.getNiveauProgramme().name())
                : VIDE;
        String typeStructure = demande.getTypeStructure() != null
                ? LIBELLES_STRUCTURE.getOrDefault(demande.getTypeStructure(), demande.getTypeStructure().name())
                : VIDE;
        String dateDecision = demande.getDateDecision() != null
                ? demande.getDateDecision().format(DATE_FORMAT)
                : VIDE;
        String dateExpiration = demande.getDateExpiration() != null
                ? demande.getDateExpiration().format(DATE_FORMAT)
                : VIDE;

        ajouterLigne(table, "Niveau du programme", niveau);
        ajouterLigne(table, "Type de structure", typeStructure);
        ajouterLigne(table, "Date de décision", dateDecision);
        ajouterLigne(table, "Valable jusqu'au", dateExpiration);
        ajouterLigne(table, "Statut", sousReserve ? "Accrédité sous réserve" : "Accrédité");

        return table;
    }

    private List<Element> planActions(String planActions) {
        Paragraph titreSection = new Paragraph("Plan d'actions à mettre en œuvre :",
                police(11, Font.BOLD, ORANGE_RESERVE));
        titreSection.setSpacingAfter(6f);

        Paragraph texte = new Paragraph(planActions, police(9.5f, Font.NORMAL, GRIS_TEXTE));
        texte.setSpacingAfter(16f);

        return List.of(titreSection, texte);
    }

    private Element signatureEtCachet(DemandeAccreditation demande) {
        String dateDecision = demande.getDateDecision() != null
                ? demande.getDateDecision().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                : VIDE;

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);

        PdfPCell c1 = new PdfPCell(new Paragraph("Dakar, le " + dateDecision, police(9, Font.ITALIC, GRIS_TEXTE)));
        c1.setBorder(0);

        PdfPCell c2 = new PdfPCell(new Paragraph(
                "La Direction de la Régulation et du Contrôle Qualité (DRCQ)\nINPG Sénégal",
                police(10, Font.BOLD, BLEU_FONCE)));
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c2.setBorder(0);

        table.addCell(c1);
        table.addCell(c2);
        return table;
    }

    /** QR de vérification publique par référence, centré sous la signature — référence en clair en secours. */
    private List<Element> verificationQr(String reference) throws Exception {
        byte[] png = genererQrCode(frontendUrl + "/verification-accreditation/" + reference);
        Image qr = Image.getInstance(png);
        qr.scaleAbsolute(60f, 60f);
        qr.setAlignment(Element.ALIGN_CENTER);
        qr.setSpacingBefore(18f);

        Paragraph legende = new Paragraph("Vérifier cette attestation — référence " + reference,
                police(7.5f, Font.ITALIC, GRIS_TEXTE));
        legende.setAlignment(Element.ALIGN_CENTER);
        legende.setSpacingBefore(4f);

        return List.of(qr, legende);
    }

    private byte[] genererQrCode(String contenu) throws Exception {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(contenu, BarcodeFormat.QR_CODE, 300, 300);
        BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", sortie);
        return sortie.toByteArray();
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
