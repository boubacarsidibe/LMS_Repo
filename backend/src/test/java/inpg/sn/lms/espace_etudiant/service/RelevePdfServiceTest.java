package inpg.sn.lms.espace_etudiant.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.espace_etudiant.entity.ReleveLigne;
import inpg.sn.lms.espace_etudiant.entity.ReleveNotes;

/**
 * {@link RelevePdfService} : le relevé délivré (valeurs figées, migration V9)
 * doit produire un PDF valide, y compris quand une ligne contient un caractère
 * hors WinAnsi — l'ancien patron de mise en page levait alors une exception.
 */
class RelevePdfServiceTest {

    private final RelevePdfService service = new RelevePdfService();

    private ReleveNotes releveComplet() {
        Etudiant etudiant = new Etudiant();
        etudiant.setPrenom("Awa");
        etudiant.setNom("Diop");
        etudiant.setMatricule("MAT-2026-001");

        ReleveNotes releve = new ReleveNotes();
        releve.setEtudiant(etudiant);
        releve.setPeriode("Semestre 1 — 2025/2026");
        releve.setDelivrePar("INPG — Direction pédagogique");
        releve.setMoyenneGenerale(14.25);
        releve.setNombreNotes(3);

        ReleveLigne ligne = new ReleveLigne();
        ligne.setCoursCode("GEO-101");
        ligne.setCoursTitre("Géologie du pétrole 🛢 avancée");
        ligne.setCoefficient(2);
        ligne.setMoyenne(15.0);
        releve.ajouterLigne(ligne);
        return releve;
    }

    @Test
    @DisplayName("génère un PDF non vide et bien formé")
    void genere_un_pdf_valide() {
        byte[] pdf = service.genererPdf(releveComplet());

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("un relevé sans lignes ni moyenne reste générable")
    void releve_vide_reste_generable() {
        ReleveNotes releve = new ReleveNotes();
        releve.setEtudiant(new Etudiant());
        releve.setPeriode("Année 2025/2026");
        releve.setDelivrePar("INPG");
        releve.setNombreNotes(0);

        byte[] pdf = service.genererPdf(releve);

        assertThat(pdf).isNotEmpty();
    }

    @Test
    @DisplayName("reponsePdf : pièce jointe nommée d'après la référence")
    void reponse_pdf_pose_le_nom_de_fichier() {
        ReleveNotes releve = releveComplet();
        // La référence est normalement posée par @PrePersist ; on la fixe pour le test.
        try {
            var champ = ReleveNotes.class.getDeclaredField("reference");
            champ.setAccessible(true);
            champ.set(releve, "INPG-REL-2026-ABCD1234");
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }

        var reponse = service.reponsePdf(releve);

        assertThat(reponse.getStatusCode().value()).isEqualTo(200);
        assertThat(reponse.getHeaders().getFirst("Content-Disposition"))
                .contains("attachment").contains("INPG-releve-INPG-REL-2026-ABCD1234.pdf");
    }

    @Test
    @DisplayName("un relevé de plusieurs pages : intitulés complets, en-tête du tableau répété, filigrane et papier à en-tête sur chaque page")
    void releve_long_reste_lisible_sur_chaque_page() throws Exception {
        ReleveNotes releve = releveComplet();
        try {
            var champ = ReleveNotes.class.getDeclaredField("reference");
            champ.setAccessible(true);
            champ.set(releve, "INPG-REL-2026-ABCD1234");
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        for (int i = 1; i <= 60; i++) {
            ReleveLigne ligne = new ReleveLigne();
            ligne.setCoursCode("C-" + i);
            ligne.setCoursTitre("Cours " + i + " : maintenance prédictive des machines tournantes et des équipements de "
                    + "production pour les opérateurs et ingénieurs de champ pétrolier (FIN-" + i + ")");
            ligne.setCoefficient(1 + i % 3);
            ligne.setMoyenne(50.0 + i % 40);
            releve.ajouterLigne(ligne);
        }

        byte[] pdf = service.genererPdf(releve);

        try (PDDocument document = Loader.loadPDF(pdf)) {
            int pages = document.getNumberOfPages();
            assertThat(pages).isGreaterThan(1);
            for (int page = 1; page <= pages; page++) {
                PDFTextStripper extracteur = new PDFTextStripper();
                extracteur.setStartPage(page);
                extracteur.setEndPage(page);
                String texte = extracteur.getText(document).replaceAll("\\s+", " ");
                assertThat(texte).as("page %d", page)
                        .contains("Intitulé du cours")
                        .contains("Immeuble Adja Koone")
                        .contains("Page " + page + " / " + pages);
            }
            String tout = new PDFTextStripper().getText(document).replaceAll("\\s+", " ");
            assertThat(tout).contains("(FIN-60)").doesNotContain("...").contains("Mention");
        }
    }
}
