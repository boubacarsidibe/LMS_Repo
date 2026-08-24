package inpg.sn.lms.espace_etudiant.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.espace_etudiant.dto.CertificatVerificationDto;
import inpg.sn.lms.espace_etudiant.entity.Certificat;
import inpg.sn.lms.espace_etudiant.entity.Promotion;
import inpg.sn.lms.espace_etudiant.service.CertificatService;

/**
 * Endpoint public de vérification d'un certificat (QR code).
 *
 * <p>Deux comportements fixés ici : une référence bien formée mais inconnue et une
 * référence malformée renvoient toutes deux un 404 sans distinction, pour ne pas
 * laisser un appelant anonyme deviner si une référence existe presque. Et la
 * réponse ne doit jamais porter l'identifiant technique ni le matricule de
 * l'étudiant — seul {@link CertificatVerificationDto} doit sortir du contrôleur.
 */
@ExtendWith(MockitoExtension.class)
class VerificationCertificatControllerTest {

    @Mock
    private CertificatService certificatService;

    @InjectMocks
    private VerificationCertificatController controller;

    @Test
    void une_reference_bien_formee_et_connue_renvoie_les_informations_publiques() {
        Etudiant etudiant = new Etudiant();
        etudiant.setPrenom("Awa");
        etudiant.setNom("Diallo");
        etudiant.setMatricule("MPG-2026-014");

        Promotion promotion = new Promotion();
        promotion.setLibelle("Master Pétrole & Gaz 2026");

        Certificat certificat = new Certificat();
        certificat.setEtudiant(etudiant);
        certificat.setPromotion(promotion);
        certificat.setIntituleFormation("Master Pétrole & Gaz");
        certificat.setDateEmission(LocalDate.of(2026, 7, 1));
        certificat.setFormateurNom("Ibrahima Ndiaye");

        when(certificatService.findByReference("INPG-CERT-2026-0A1B2C3D"))
                .thenReturn(Optional.of(certificat));

        ResponseEntity<CertificatVerificationDto> reponse = controller.verifier("INPG-CERT-2026-0A1B2C3D");

        assertEquals(HttpStatus.OK, reponse.getStatusCode());
        CertificatVerificationDto corps = reponse.getBody();
        assertEquals("Awa Diallo", corps.etudiantNom());
        assertEquals("Master Pétrole & Gaz", corps.intituleFormation());
        assertEquals("Master Pétrole & Gaz 2026", corps.promotionLibelle());
        assertEquals("Ibrahima Ndiaye", corps.formateurNom());
    }

    @Test
    void une_reference_bien_formee_mais_inconnue_renvoie_404() {
        when(certificatService.findByReference("INPG-CERT-2026-FFFFFFFF")).thenReturn(Optional.empty());

        ResponseEntity<CertificatVerificationDto> reponse = controller.verifier("INPG-CERT-2026-FFFFFFFF");

        assertEquals(HttpStatus.NOT_FOUND, reponse.getStatusCode());
        assertNull(reponse.getBody());
    }

    @Test
    void une_reference_malformee_renvoie_404_sans_interroger_la_base() {
        ResponseEntity<CertificatVerificationDto> reponse = controller.verifier("'; DROP TABLE certificat; --");

        assertEquals(HttpStatus.NOT_FOUND, reponse.getStatusCode());
        verifyNoInteractions(certificatService);
    }
}
