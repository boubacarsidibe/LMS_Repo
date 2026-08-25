package inpg.sn.lms.recrutement.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import inpg.sn.lms.auth.entity.Candidat;
import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.recrutement.entity.Candidature;
import inpg.sn.lms.recrutement.entity.Paiement;
import inpg.sn.lms.recrutement.entity.SessionCandidature;
import inpg.sn.lms.recrutement.enums.EnumStatutPaiement;

class RecuPaiementServiceTest {

    private final RecuPaiementService service = new RecuPaiementService();

    @Test
    void genere_un_pdf_valide_pour_un_paiement_complet() {
        byte[] pdf = service.genererRecu(paiementComplet());

        assertNotNull(pdf);
        assertTrue(pdf.length > 0, "le reçu ne doit pas être vide");
        assertTrue(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1).startsWith("%PDF-"),
                "le fichier doit porter la signature PDF");
    }

    /**
     * Les paiements créés avant l'ajout de la colonne methodePaiement, ou dont le
     * dossier est incomplet, doivent produire un reçu lisible plutôt qu'une erreur.
     */
    @Test
    void genere_un_pdf_meme_avec_des_donnees_manquantes() {
        Paiement paiement = new Paiement();
        paiement.setReferenceTransaction("PAY-VIDE");
        paiement.setStatut(EnumStatutPaiement.SUCCES);

        byte[] pdf = service.genererRecu(paiement);

        assertTrue(pdf.length > 0);
        assertTrue(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1).startsWith("%PDF-"));
    }

    private Paiement paiementComplet() {
        User user = new User();
        user.setEmail("awa.diallo@example.sn");

        Candidat candidat = new Candidat();
        candidat.setNom("Diallo");
        candidat.setPrenom("Awa");
        candidat.setUser(user);

        SessionCandidature session = new SessionCandidature();
        session.setLibelle("Master Génie Logiciel — Rentrée 2026");

        Candidature candidature = new Candidature();
        candidature.setNumeroDossier("DOS-2026-0042");
        candidature.setCandidat(candidat);
        candidature.setSessionCandidature(session);

        Paiement paiement = new Paiement();
        paiement.setReferenceTransaction("PAY-8F2C1A9B");
        paiement.setMontant(new BigDecimal("15000"));
        paiement.setDevise("XOF");
        paiement.setStatut(EnumStatutPaiement.SUCCES);
        paiement.setDatePaiement(LocalDateTime.of(2026, 8, 13, 10, 30));
        paiement.setMethodePaiement("Wave");
        paiement.setCandidature(candidature);
        return paiement;
    }
}
