package inpg.sn.lms.recrutement.service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import java.util.List;

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

import inpg.sn.lms.pdf.PapierEnTete;
import inpg.sn.lms.pdf.PapierEnTeteOpenPdf;
import inpg.sn.lms.auth.entity.Candidat;
import inpg.sn.lms.recrutement.entity.Candidature;
import inpg.sn.lms.recrutement.entity.Paiement;

/**
 * Genere le recu PDF des frais de dossier. Pendant du {@link ExcelExportService}
 * pour les documents PDF : produit un {@code byte[]} a partir des entites, sans
 * dependance au web, pour etre reutilisable cote candidat comme cote back-office.
 */
@Service
public class RecuPaiementService {

    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");

    /** Valeur affichee quand la donnee est absente (tiret cadratin). */
    private static final String VIDE = "—";

    /** Bleu de la charte (#30609a), aligne sur l'interface. */
    private static final java.awt.Color GRIS_TEXTE = new java.awt.Color(0x46, 0x46, 0x4e);
    private static final java.awt.Color GRIS_FOND = new java.awt.Color(0xf2, 0xf4, 0xf6);
    private static final java.awt.Color GRIS_BORDURE = new java.awt.Color(0xc7, 0xc5, 0xd0);

    public byte[] genererRecu(Paiement paiement) {
        Candidature candidature = paiement.getCandidature();
        Candidat candidat = candidature != null ? candidature.getCandidat() : null;

        Document document = new Document(PageSize.A4, PapierEnTete.MARGE_GAUCHE, PapierEnTete.MARGE_DROITE,
                PapierEnTete.HAUT_CONTENU, PapierEnTete.BAS_CONTENU);
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, sortie).setPageEvent(new PapierEnTeteOpenPdf());
            document.open();

            PapierEnTeteOpenPdf.ajouter(document, titre(candidature));
            document.add(tableauDetails(paiement, candidature, candidat));
            document.add(mentionLegale());

            document.close();
        } catch (Exception e) {
            // RuntimeException nue : une panne de generation est une erreur serveur (500),
            // pas une requete invalide (IllegalStateException serait traduit en 400 par
            // GlobalExceptionHandler).
            throw new RuntimeException("Impossible de generer le recu de paiement", e);
        }
        return sortie.toByteArray();
    }

    private List<Element> titre(Candidature candidature) {
        Paragraph titre = new Paragraph("Reçu de paiement " + VIDE + " Frais de dossier",
                police(15, Font.BOLD, java.awt.Color.BLACK));
        titre.setSpacingAfter(4f);

        String dossier = candidature != null ? nvl(candidature.getNumeroDossier()) : VIDE;
        Paragraph sousTitre = new Paragraph("Dossier n° " + dossier, police(11, Font.NORMAL, GRIS_TEXTE));
        sousTitre.setSpacingAfter(20f);

        return List.of(titre, sousTitre);
    }

    private Element tableauDetails(Paiement paiement, Candidature candidature, Candidat candidat) throws Exception {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 38f, 62f });
        table.setSpacingAfter(28f);

        ajouterLigne(table, "Candidat", nomComplet(candidat));
        ajouterLigne(table, "Email", email(candidat));
        ajouterLigne(table, "Formation",
                candidature != null && candidature.getSessionCandidature() != null
                        ? nvl(candidature.getSessionCandidature().getLibelle())
                        : VIDE);
        ajouterLigne(table, "Référence de transaction", nvl(paiement.getReferenceTransaction()));
        ajouterLigne(table, "Date du paiement",
                paiement.getDatePaiement() != null ? paiement.getDatePaiement().format(DATETIME_FORMAT) : VIDE);
        ajouterLigne(table, "Méthode de paiement", nvl(paiement.getMethodePaiement()));
        ajouterLigne(table, "Montant réglé", montantFormate(paiement));
        ajouterLigne(table, "Statut", "Paiement validé");

        return table;
    }

    /** Libelle a gauche sur fond gris, valeur a droite : mise en forme unique pour tout le tableau. */
    private void ajouterLigne(PdfPTable table, String libelle, String valeur) {
        PdfPCell cellLibelle = new PdfPCell(new Phrase(libelle, police(10, Font.BOLD, GRIS_TEXTE)));
        cellLibelle.setBackgroundColor(GRIS_FOND);
        cellLibelle.setBorderColor(GRIS_BORDURE);
        cellLibelle.setPadding(8f);

        PdfPCell cellValeur = new PdfPCell(new Phrase(valeur, police(10, Font.NORMAL, java.awt.Color.BLACK)));
        cellValeur.setBorderColor(GRIS_BORDURE);
        cellValeur.setPadding(8f);

        table.addCell(cellLibelle);
        table.addCell(cellValeur);
    }

    private Element mentionLegale() {
        Paragraph mention = new Paragraph(
                "Ce reçu est généré automatiquement par la plateforme d'admission de l'INPG "
                        + "et ne nécessite pas de signature. Il atteste du règlement des frais de dossier "
                        + "et ne préjuge pas de la décision d'admission.",
                police(9, Font.ITALIC, GRIS_TEXTE));
        mention.setAlignment(Element.ALIGN_JUSTIFIED);
        return mention;
    }

    private String montantFormate(Paiement paiement) {
        if (paiement.getMontant() == null) {
            return VIDE;
        }
        // Locale figee puis separateur remplace : le rendu ne doit pas dependre de la
        // locale par defaut de la JVM qui execute le serveur.
        String montant = String.format(Locale.US, "%,.0f", paiement.getMontant()).replace(',', ' ');
        return montant + " " + nvl(paiement.getDevise());
    }

    private String nomComplet(Candidat candidat) {
        if (candidat == null) {
            return VIDE;
        }
        String complet = Stream.of(candidat.getPrenom(), candidat.getNom())
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(" "));
        return complet.isBlank() ? VIDE : complet;
    }

    private String email(Candidat candidat) {
        if (candidat == null || candidat.getUser() == null) {
            return VIDE;
        }
        return nvl(candidat.getUser().getEmail());
    }

    private String nvl(String valeur) {
        return valeur == null || valeur.isBlank() ? VIDE : valeur;
    }

    private Font police(int taille, int style, java.awt.Color couleur) {
        return FontFactory.getFont(FontFactory.HELVETICA, taille, style, couleur);
    }
}
