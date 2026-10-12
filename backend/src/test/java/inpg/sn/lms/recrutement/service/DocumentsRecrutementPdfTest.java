package inpg.sn.lms.recrutement.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import inpg.sn.lms.auth.entity.Candidat;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.recrutement.entity.Candidature;
import inpg.sn.lms.recrutement.entity.Entretien;
import inpg.sn.lms.recrutement.entity.Paiement;
import inpg.sn.lms.recrutement.entity.SessionCandidature;
import inpg.sn.lms.recrutement.enums.EnumStatutPaiement;

/**
 * Reçu de paiement, lettre d'admission et convocation : ils portent le papier à en-tête de l'INPG (logo officiel,
 * pied de page avec adresse, téléphone et site) à la place de l'ancienne marque en texte « INPG LMS ».
 */
class DocumentsRecrutementPdfTest {

    private Candidature candidature() {
        User user = new User();
        user.setEmail("awa.diallo@example.sn");
        Candidat candidat = new Candidat();
        candidat.setNom("Diallo");
        candidat.setPrenom("Awa");
        candidat.setTelephone("+221770000001");
        candidat.setUser(user);
        SessionCandidature session = new SessionCandidature();
        session.setLibelle("Master Génie Logiciel — Rentrée 2026");
        Candidature candidature = new Candidature();
        candidature.setNumeroDossier("DOS-2026-0042");
        candidature.setCandidat(candidat);
        candidature.setSessionCandidature(session);
        candidature.setDateSoumission(LocalDateTime.of(2026, 8, 1, 9, 0));
        return candidature;
    }

    private void verifier(byte[] pdf, String contenuAttendu) throws Exception {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(document.getNumberOfPages()).as("un document d'une seule page").isEqualTo(1);
            String texte = new PDFTextStripper().getText(document).replaceAll("\\s+", " ");
            assertThat(texte)
                    .contains(contenuAttendu)
                    .contains("Immeuble Adja Koone - Boulevard de l'Est, Point E, Dakar – Sénégal")
                    .contains("+221 33 865 91 48 / 49")
                    .contains("www.inpg.sn")
                    .doesNotContain("INPG LMS");

            PDPage page = document.getPage(0);
            PDResources ressources = page.getResources();
            long images = 0;
            for (COSName nom : ressources.getXObjectNames()) {
                images += ressources.isImageXObject(nom) ? 1 : 0;
            }
            assertThat(images).as("logo + deux icônes du pied de page").isGreaterThanOrEqualTo(3);
        }
    }

    @Test
    @DisplayName("la lettre d'admission porte le papier à en-tête, sur une seule page")
    void lettre_d_admission() throws Exception {
        verifier(new LettreAdmissionPdfService().genererLettreAdmission(candidature()), "NOTIFICATION OFFICIELLE D'ADMISSION");
    }

    @Test
    @DisplayName("la convocation porte le papier à en-tête, sur une seule page")
    void convocation() throws Exception {
        Entretien entretien = new Entretien();
        entretien.setCandidat(candidature().getCandidat());
        entretien.setDateHeure(LocalDateTime.of(2026, 9, 10, 9, 30));
        entretien.setLieuOuLien("Salle 12 — siège de l'INPG");

        verifier(new ConvocationPdfService().genererConvocation(entretien, candidature()), "CONVOCATION OFFICIELLE");
    }

    @Test
    @DisplayName("le reçu de paiement porte le papier à en-tête, sur une seule page")
    void recu_de_paiement() throws Exception {
        Paiement paiement = new Paiement();
        paiement.setReferenceTransaction("PAY-8F2C1A9B");
        paiement.setMontant(new BigDecimal("15000"));
        paiement.setDevise("XOF");
        paiement.setStatut(EnumStatutPaiement.SUCCES);
        paiement.setDatePaiement(LocalDateTime.of(2026, 8, 13, 10, 30));
        paiement.setMethodePaiement("Wave");
        paiement.setCandidature(candidature());

        verifier(new RecuPaiementService().genererRecu(paiement), "Reçu de paiement");
    }
}
