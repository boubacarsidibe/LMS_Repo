package inpg.sn.lms.espace_etudiant.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

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
}
