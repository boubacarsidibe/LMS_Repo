package inpg.sn.lms.espace_etudiant.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.espace_etudiant.entity.Certificat;

/**
 * {@link CertificatPdfService} : le PDF reprend le modèle officiel de l'INPG (textes fixes du fond) et y
 * superpose les seules valeurs variables. Les tests relisent le texte du PDF produit plutôt que de
 * seulement constater qu'un fichier est généré.
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
        certificat.setIntituleFormation("Comptage du gaz");
        certificat.setFormateurNom("INPG — Direction pédagogique");
        certificat.setDateEmission(LocalDate.of(2026, 2, 6));
        certificat.assurerReferenceEtDate();
        return certificat;
    }

    /** Le certificat de l'exemple du modèle : Monsieur Cherif Tidiane AIDARA, Comptage du gaz. */
    private Certificat certificatDuModele() {
        Certificat certificat = certificat("Cherif Tidiane", "Aidara");
        certificat.setCivilite("Monsieur");
        certificat.setDateNaissance(LocalDate.of(1985, 8, 24));
        certificat.setDateDebut(LocalDate.of(2026, 2, 2));
        certificat.setDateFin(LocalDate.of(2026, 2, 6));
        certificat.setCodeCertificat("CG1001260206/03");
        return certificat;
    }

    private String texte(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document).replaceAll("\\s+", " ");
        }
    }

    @Test
    @DisplayName("génère un PDF valide d'une seule page")
    void genere_un_pdf_valide() throws IOException {
        byte[] pdf = service.genererPdf(certificatDuModele());

        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(document.getNumberOfPages()).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("reprend les textes fixes du modèle et y ajoute les valeurs variables")
    void reprend_le_modele_et_ajoute_les_valeurs() throws IOException {
        String texte = texte(service.genererPdf(certificatDuModele()));

        assertThat(texte)
                .contains("CERTIFICAT DE FORMATION")
                .contains("atteste que")
                .contains("A suivi avec succès la formation intitulée")
                .contains("Le Directeur Général")
                .contains("Monsieur Cherif Tidiane AIDARA")
                .contains("né le 24/08/1985")
                .contains("Comptage du gaz")
                .contains("Du 02 au 06 février 2026")
                .contains("Fait à Dakar, le 06 février 2026")
                .contains("INPG - Attestation - RDC - CG1001260206/03");
    }

    @Test
    @DisplayName("le fond vierge ne contient aucune donnée de l'exemple du modèle")
    void le_fond_ne_porte_aucune_donnee_personnelle() throws IOException {
        Certificat autre = certificat("Awa", "Diop");
        autre.setCivilite("Madame");
        autre.setCodeCertificat("XX0000000000/01");
        String texte = texte(service.genererPdf(autre));

        assertThat(texte).doesNotContain("AIDARA", "Cherif", "1985", "CG1001260206").contains("Madame Awa DIOP");
    }

    @Test
    @DisplayName("une bénéficiaire porte « née le »")
    void feminin() throws IOException {
        Certificat certificat = certificat("Awa", "Diop");
        certificat.setCivilite("Madame");
        certificat.setDateNaissance(LocalDate.of(1990, 3, 5));

        assertThat(texte(service.genererPdf(certificat))).contains("née le 05/03/1990");
    }

    @Test
    @DisplayName("un certificat délivré avant le modèle INPG reste imprimable : ni période ni naissance, la référence tient lieu de code")
    void certificat_ancien_sans_champs_du_modele() throws IOException {
        Certificat ancien = certificat("Mamadou", "Baldé");

        String texte = texte(service.genererPdf(ancien));

        assertThat(texte)
                .contains("Mamadou BALDÉ")
                .contains("Comptage du gaz")
                .contains("INPG - Attestation - RDC - " + ancien.getReference())
                .doesNotContain("né le", "née le", "Du ");
    }

    @Test
    @DisplayName("un nom hors Latin-1 ne fait pas échouer la génération")
    void nom_exotique_est_tolere() {
        byte[] pdf = service.genererPdf(certificat("李", "Wei"));

        assertThat(pdf).isNotEmpty();
    }

    @Test
    @DisplayName("un nom et un intitulé très longs sont réduits sans lever d'exception")
    void textes_tres_longs() {
        Certificat certificat = certificatDuModele();
        certificat.setIntituleFormation("Formation avancée en maintenance prédictive des machines tournantes et des "
                + "équipements de production pour les opérateurs et ingénieurs de champ pétrolier");
        certificat.getEtudiant().setPrenom("Jean-Baptiste Emmanuel Marie Christophe Alexandre");

        assertThat(service.genererPdf(certificat)).isNotEmpty();
    }

    @Test
    @DisplayName("la période s'écrit à la française selon que le mois ou l'année changent")
    void libelle_de_periode() {
        assertThat(CertificatPdfService.libellePeriode(LocalDate.of(2026, 2, 2), LocalDate.of(2026, 2, 6)))
                .isEqualTo("Du 02 au 06 février 2026");
        assertThat(CertificatPdfService.libellePeriode(LocalDate.of(2026, 1, 28), LocalDate.of(2026, 2, 6)))
                .isEqualTo("Du 28 janvier au 06 février 2026");
        assertThat(CertificatPdfService.libellePeriode(LocalDate.of(2025, 12, 28), LocalDate.of(2026, 1, 6)))
                .isEqualTo("Du 28 décembre 2025 au 06 janvier 2026");
        assertThat(CertificatPdfService.libellePeriode(LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 3)))
                .isEqualTo("Le 03 août 2026");
        assertThat(CertificatPdfService.libellePeriode(null, null)).isNull();
    }
}
