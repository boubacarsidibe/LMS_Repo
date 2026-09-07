package inpg.sn.lms.espace_etudiant.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.espace_etudiant.entity.Certificat;

/**
 * {@link CertificatPdfService} : la mise en page restaure les accents (WinAnsi
 * les encode) et tolère un nom d'étudiant hors Latin-1 sans lever d'exception.
 */
class CertificatPdfServiceTest {

    private final CertificatPdfService service = new CertificatPdfService("https://lms.inpg.sn");

    private Certificat certificat(String prenom, String nom) {
        Etudiant etudiant = new Etudiant();
        etudiant.setPrenom(prenom);
        etudiant.setNom(nom);
        etudiant.setMatricule("MAT-2026-042");

        Certificat certificat = new Certificat();
        certificat.setEtudiant(etudiant);
        certificat.setIntituleFormation("Master Pétrole & Gaz");
        certificat.setFormateurNom("INPG — Direction pédagogique");
        return certificat;
    }

    @Test
    @DisplayName("génère un PDF valide avec une mention quand la moyenne est fournie")
    void genere_un_pdf_avec_mention() {
        byte[] pdf = service.genererPdf(certificat("Mamadou", "Baldé"), 15.0);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("un nom hors Latin-1 ne fait pas échouer la génération")
    void nom_exotique_est_tolere() {
        byte[] pdf = service.genererPdf(certificat("李", "Wei"), null);

        assertThat(pdf).isNotEmpty();
    }
}
