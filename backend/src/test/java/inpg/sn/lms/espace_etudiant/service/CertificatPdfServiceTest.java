package inpg.sn.lms.espace_etudiant.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.espace_etudiant.entity.Certificat;

/**
 * Le QR code du PDF doit pointer vers la page publique de vérification, pas
 * afficher un simple texte : c'est ce lien qui permet à un tiers de contrôler
 * l'authenticité d'un certificat sans compte sur la plateforme (TDR §B).
 */
class CertificatPdfServiceTest {

    @Test
    void le_qr_code_encode_lurl_de_verification_avec_la_reference() throws Exception {
        CertificatPdfService service = new CertificatPdfService("https://lms.inpg.sn");

        Etudiant etudiant = new Etudiant();
        etudiant.setPrenom("Awa");
        etudiant.setNom("Diallo");

        Certificat certificat = new Certificat();
        certificat.setEtudiant(etudiant);
        certificat.setIntituleFormation("Master Pétrole & Gaz");
        certificat.setDateEmission(LocalDate.of(2026, 7, 1));
        certificat.setFormateurNom("Ibrahima Ndiaye");
        // Pas de setter public : la référence n'est normalement écrite qu'au premier
        // persist (Certificat#genererReferenceSiAbsente), inapplicable hors contexte JPA.
        ReflectionTestUtils.setField(certificat, "reference", "INPG-CERT-2026-0A1B2C3D");

        byte[] pdf = service.genererPdf(certificat);

        String contenuQr = decoderQrDuPdf(pdf);
        assertEquals("https://lms.inpg.sn/verification-certificat/INPG-CERT-2026-0A1B2C3D", contenuQr);
    }

    private String decoderQrDuPdf(byte[] pdf) throws Exception {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDFRenderer renderer = new PDFRenderer(document);
            var image = renderer.renderImageWithDPI(0, 150);

            var sortie = new java.io.ByteArrayOutputStream();
            ImageIO.write(image, "PNG", sortie);
            var relue = ImageIO.read(new ByteArrayInputStream(sortie.toByteArray()));

            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(relue)));
            Map<DecodeHintType, Object> hints = new HashMap<>();
            hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
            Result resultat = new MultiFormatReader().decode(bitmap, hints);
            assertTrue(resultat.getText().startsWith("https://"));
            return resultat.getText();
        }
    }
}
