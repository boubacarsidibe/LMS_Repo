package inpg.sn.lms.accreditation.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import inpg.sn.lms.accreditation.entity.DemandeAccreditation;
import inpg.sn.lms.accreditation.enums.EnumNiveauProgramme;
import inpg.sn.lms.accreditation.enums.EnumStatutDemandeAccreditation;
import inpg.sn.lms.accreditation.enums.EnumTypeStructure;
import inpg.sn.lms.auth.entity.EcoleFormation;
import inpg.sn.lms.auth.entity.User;

/**
 * {@link AttestationAccreditationPdfService} : contrairement au certificat de formation, aucun modèle
 * officiel n'existe pour ce document — il est entièrement mis en page par l'application (papier à
 * en-tête commun). Les tests relisent le texte du PDF produit plutôt que de seulement constater qu'un
 * fichier est généré (même règle que CertificatPdfServiceTest, imposée après un défaut du même genre
 * passé inaperçu sur d'autres rapports).
 */
class AttestationAccreditationPdfServiceTest {

    private final AttestationAccreditationPdfService service = new AttestationAccreditationPdfService("https://lms.inpg.sn");

    private DemandeAccreditation demande(EnumStatutDemandeAccreditation statut) {
        User ecoleUser = new User();
        ecoleUser.setId(1L);
        EcoleFormation ecole = new EcoleFormation(ecoleUser, "Institut Supérieur du Pétrole", "Adresse", "77000000", null);

        DemandeAccreditation demande = new DemandeAccreditation(ecole);
        demande.setIntituleProgramme("Technicien en forage");
        demande.setNiveauProgramme(EnumNiveauProgramme.TECHNICIEN);
        demande.setTypeStructure(EnumTypeStructure.ECOLE_INGENIEURS);
        demande.setStatut(statut);
        demande.setDateDecision(LocalDateTime.of(2026, 9, 27, 10, 0));
        demande.setDateExpiration(LocalDate.of(2029, 9, 27));
        demande.setReferenceAttestation("INPG-ACC-2026-ABCDEF01");
        return demande;
    }

    private String texte(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document).replaceAll("\\s+", " ");
        }
    }

    @Test
    @DisplayName("génère un PDF valide")
    void genere_un_pdf_valide() throws IOException {
        byte[] pdf = service.genererPdf(demande(EnumStatutDemandeAccreditation.ACCREDITE));

        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(document.getNumberOfPages()).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("accréditation pleine : école, programme, niveau et validité sont imprimés")
    void accreditation_pleine() throws IOException {
        String texte = texte(service.genererPdf(demande(EnumStatutDemandeAccreditation.ACCREDITE)));

        assertThat(texte)
                .contains("ATTESTATION D'ACCRÉDITATION")
                .contains("DÉCISION : ACCRÉDITÉ")
                .doesNotContain("SOUS RÉSERVE")
                .contains("Institut Supérieur du Pétrole")
                .contains("Technicien en forage")
                .contains("Technicien")
                .contains("École d'ingénieurs")
                .contains("27 septembre 2026")
                .contains("27 septembre 2029")
                .contains("Direction de la Régulation et du Contrôle Qualité")
                .as("la référence est imprimée sous le QR pour une saisie manuelle")
                .contains("INPG-ACC-2026-ABCDEF01");
    }

    @Test
    @DisplayName("accréditation sous réserve : le badge et le plan d'actions apparaissent")
    void accreditation_sous_reserve() throws IOException {
        DemandeAccreditation demande = demande(EnumStatutDemandeAccreditation.ACCREDITE_SOUS_RESERVE);
        demande.setPlanActions("Recruter un formateur supplémentaire avant la prochaine cohorte.");

        String texte = texte(service.genererPdf(demande));

        assertThat(texte)
                .contains("DÉCISION : ACCRÉDITÉ SOUS RÉSERVE")
                .contains("Plan d'actions à mettre en œuvre")
                .contains("Recruter un formateur supplémentaire avant la prochaine cohorte.");
    }

    @Test
    @DisplayName("accréditation pleine : aucun plan d'actions imprimé, même si le champ est renseigné par erreur")
    void pas_de_plan_actions_hors_sous_reserve() throws IOException {
        DemandeAccreditation demande = demande(EnumStatutDemandeAccreditation.ACCREDITE);
        demande.setPlanActions("Ne devrait jamais apparaître ici.");

        String texte = texte(service.genererPdf(demande));

        assertThat(texte).doesNotContain("Ne devrait jamais apparaître ici.");
    }

    @Test
    @DisplayName("sans référence encore générée : le PDF reste imprimable, sans bloc de vérification")
    void sans_reference() throws IOException {
        DemandeAccreditation demande = demande(EnumStatutDemandeAccreditation.ACCREDITE);
        demande.setReferenceAttestation(null);

        String texte = texte(service.genererPdf(demande));

        assertThat(texte).contains("Technicien en forage").doesNotContain("Vérifier cette attestation");
    }
}
