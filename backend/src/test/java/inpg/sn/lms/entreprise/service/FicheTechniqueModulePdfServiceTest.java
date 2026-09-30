package inpg.sn.lms.entreprise.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import inpg.sn.lms.auth.entity.Entreprise;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.entreprise.entity.DemandeEntreprise;
import inpg.sn.lms.entreprise.entity.FicheModuleJour;
import inpg.sn.lms.entreprise.entity.FicheTechniqueModule;
import inpg.sn.lms.entreprise.enums.EnumTypeDemandeEntreprise;

/**
 * {@link FicheTechniqueModulePdfService} : ni le devis ni la convention n'ont de modèle officiel de
 * l'INPG — les tests relisent donc le texte des PDF produits, même règle que pour le certificat de
 * formation et l'attestation d'accréditation (une inspection « un fichier est généré » ne verrait pas
 * une section manquante ou mal recopiée).
 */
class FicheTechniqueModulePdfServiceTest {

    private final FicheTechniqueModulePdfService service = new FicheTechniqueModulePdfService();

    private FicheTechniqueModule ficheComplete() {
        User user = new User();
        user.setId(1L);
        Entreprise entreprise = new Entreprise(user, "Total Énergies Sénégal", "Pétrolier", "Adresse", "770000000");
        DemandeEntreprise demande = new DemandeEntreprise(entreprise, EnumTypeDemandeEntreprise.DEVIS_SESSION, null,
                "Formation sur mesure", 12, "Q1 2027");

        FicheTechniqueModule fiche = new FicheTechniqueModule(demande);
        fiche.setCode("FTM-2026-014");
        fiche.setIntitule("Lubrification des machines tournantes");
        fiche.setResponsableModule("Cherif Tidiane Aidara");
        fiche.setDepartement("Maintenance");
        fiche.setAtelierService("Atelier rotatif");
        fiche.setFonctionsCibles("Techniciens de maintenance");
        fiche.setThematiques("Lubrification, vibrations, alignement");
        fiche.setPrerequis("Bases de mécanique industrielle");
        fiche.setNiveau("Technicien");
        fiche.setEffectif(12);
        fiche.setDureeJours(3);
        fiche.setDureeHeures(21);
        fiche.setPeriodeProbable("Février 2027");
        fiche.setObjectifGeneral("Maîtriser les bases de la lubrification industrielle");
        fiche.setObjectifsSpecifiques("Identifier les points de lubrification critiques");
        fiche.setModalitesTestPrerequis("Questionnaire d'entrée");
        fiche.setFormationSurSite("Oui, sur le site de l'entreprise");
        fiche.setTypesEvaluation("QCM final et évaluation pratique");
        fiche.setRessourcesPedagogiques("Support papier, simulateur");
        fiche.setBibliographie("Manuel de lubrification industrielle, 3e édition");
        fiche.setProfilFormateur("Ingénieur mécanique, 10 ans d'expérience");
        fiche.setCoutTotal("2 500 000 FCFA");
        fiche.setModalitesFinancieres("50% à la commande, 50% à la livraison");
        fiche.getJours().add(new FicheModuleJour(fiche, 1, "Théorie de la lubrification", 7, "Présentiel", "Salle A"));
        fiche.getJours().add(new FicheModuleJour(fiche, 2, "Travaux pratiques", 7, "Atelier", "Sur site"));
        return fiche;
    }

    private String texte(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document).replaceAll("\\s+", " ");
        }
    }

    // ===================== Devis =====================

    @Test
    @DisplayName("Devis : PDF valide")
    void devis_pdf_valide() throws IOException {
        byte[] pdf = service.genererDevisPdf(ficheComplete());

        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("Devis : entreprise, intitulé, effectif, durée et coût sont imprimés")
    void devis_contient_les_champs_commerciaux() throws IOException {
        String texte = texte(service.genererDevisPdf(ficheComplete()));

        assertThat(texte)
                .contains("PROPOSITION COMMERCIALE")
                .contains("Total Énergies Sénégal")
                .contains("Lubrification des machines tournantes")
                .contains("12 personne(s)")
                .contains("3 j")
                .contains("21 h")
                .contains("2 500 000 FCFA")
                .contains("50% à la commande, 50% à la livraison");
    }

    @Test
    @DisplayName("Devis : ne reprend pas le planning jour par jour (réservé à la convention)")
    void devis_omet_le_planning() throws IOException {
        String texte = texte(service.genererDevisPdf(ficheComplete()));

        assertThat(texte).doesNotContain("Théorie de la lubrification").doesNotContain("Travaux pratiques");
    }

    // ===================== Convention =====================

    @Test
    @DisplayName("Convention : PDF valide")
    void convention_pdf_valide() throws IOException {
        byte[] pdf = service.genererConventionPdf(ficheComplete());

        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("Convention : toutes les sections renseignées sont imprimées, avec le planning")
    void convention_contient_toutes_les_sections() throws IOException {
        String texte = texte(service.genererConventionPdf(ficheComplete()));

        assertThat(texte)
                .contains("CONVENTION DE FORMATION")
                .contains("Total Énergies Sénégal")
                .contains("FTM-2026-014")
                .contains("Maintenance")
                .contains("Atelier rotatif")
                .contains("Techniciens de maintenance")
                .contains("Maîtriser les bases de la lubrification industrielle")
                .contains("Identifier les points de lubrification critiques")
                .contains("Questionnaire d'entrée")
                .contains("Oui, sur le site de l'entreprise")
                .contains("QCM final et évaluation pratique")
                .contains("Support papier, simulateur")
                .contains("Manuel de lubrification industrielle, 3e édition")
                .contains("Ingénieur mécanique, 10 ans d'expérience")
                .contains("2 500 000 FCFA")
                .contains("Théorie de la lubrification")
                .contains("Travaux pratiques")
                .contains("Cherif Tidiane Aidara");
    }

    @Test
    @DisplayName("Convention : une fiche minimale (seuls les champs obligatoires) ne fait pas échouer la génération, "
            + "et les sections vides n'apparaissent pas")
    void convention_avec_fiche_minimale() throws IOException {
        User user = new User();
        user.setId(2L);
        Entreprise entreprise = new Entreprise(user, "Petite Entreprise", null, "Adresse", "770000001");
        DemandeEntreprise demande = new DemandeEntreprise(entreprise, EnumTypeDemandeEntreprise.DEVIS_SESSION, null,
                "Besoin", null, null);
        FicheTechniqueModule fiche = new FicheTechniqueModule(demande);
        fiche.setIntitule("Formation minimale");
        fiche.setObjectifGeneral("Objectif minimal");
        fiche.getJours().add(new FicheModuleJour(fiche, 1, "Contenu", null, null, null));

        String texte = texte(service.genererConventionPdf(fiche));

        assertThat(texte).contains("Petite Entreprise").contains("Formation minimale").contains("Objectif minimal");
        assertThat(texte).doesNotContain("Bibliographie").doesNotContain("Profil des formateurs");
    }

    @Test
    @DisplayName("Convention : sans coût renseigné, aucune section financière n'apparaît")
    void convention_sans_cout_omet_la_section_financiere() throws IOException {
        FicheTechniqueModule fiche = ficheComplete();
        fiche.setCoutTotal(null);
        fiche.setModalitesFinancieres(null);

        String texte = texte(service.genererConventionPdf(fiche));

        assertThat(texte).doesNotContain("Coût total").doesNotContain("Modalités financières");
    }
}
