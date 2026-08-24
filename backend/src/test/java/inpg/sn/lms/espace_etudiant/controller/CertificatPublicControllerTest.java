package inpg.sn.lms.espace_etudiant.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.espace_etudiant.dto.CertificatVerificationDto;
import inpg.sn.lms.espace_etudiant.entity.Certificat;
import inpg.sn.lms.espace_etudiant.service.CertificatService;
import inpg.sn.lms.espace_etudiant.service.CertificatSignatureService;

/**
 * Vérification publique d'un certificat.
 *
 * <p>Deux garanties de sécurité sont couvertes ici, pas seulement le chemin
 * heureux : une référence inconnue renvoie 404 sans détail (pas de distinction
 * entre "n'existe pas" et "existe mais pas le droit"), et la réponse n'expose
 * jamais le matricule ni un identifiant technique de l'étudiant — seules les
 * informations déjà imprimées sur le certificat.
 */
@ExtendWith(MockitoExtension.class)
class CertificatPublicControllerTest {

    @Mock
    private CertificatService certificatService;

    @Mock
    private CertificatSignatureService signatureService;

    private CertificatPublicController controller;

    @BeforeEach
    void setUp() {
        controller = new CertificatPublicController(certificatService, signatureService);
    }

    @Test
    @DisplayName("Référence inconnue : 404, sans exposer d'information")
    void referenceInconnueRenvoie404() {
        when(certificatService.findByReference("INPG-CERT-2026-INCONNU")).thenReturn(Optional.empty());

        ResponseEntity<CertificatVerificationDto> reponse = controller.verifier("INPG-CERT-2026-INCONNU");

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(reponse.getBody()).isNull();
    }

    @Test
    @DisplayName("Certificat valide : la réponse est cohérente et ne fuite pas le matricule")
    void certificatValideRenvoieLesChampsPublics() {
        Etudiant etudiant = new Etudiant(null, "Ndiaye", "Aissatou", "770000000");
        etudiant.setMatricule("MAT-2026-001");

        Certificat certificat = new Certificat();
        certificat.setEtudiant(etudiant);
        certificat.setIntituleFormation("Introduction au Machine Learning");
        certificat.setFormateurNom("INPG — Direction pédagogique");
        certificat.assurerReferenceEtDate();
        certificat.setSignature("abc123");

        when(certificatService.findByReference(certificat.getReference())).thenReturn(Optional.of(certificat));
        when(signatureService.estAlteree(certificat)).thenReturn(false);

        ResponseEntity<CertificatVerificationDto> reponse = controller.verifier(certificat.getReference());

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        CertificatVerificationDto corps = reponse.getBody();
        assertThat(corps).isNotNull();
        assertThat(corps.reference()).isEqualTo(certificat.getReference());
        assertThat(corps.etudiantNomComplet()).isEqualTo("Aissatou Ndiaye");
        assertThat(corps.verifiable()).isTrue();
        assertThat(corps.valide()).isTrue();
        assertThat(corps.toString()).doesNotContain("MAT-2026-001");
    }

    @Test
    @DisplayName("Certificat dont le contenu a été altéré après signature : valide=false")
    void certificatAltereEstSignaleInvalide() {
        Etudiant etudiant = new Etudiant(null, "Ndiaye", "Aissatou", "770000000");
        Certificat certificat = new Certificat();
        certificat.setEtudiant(etudiant);
        certificat.setIntituleFormation("Introduction au Machine Learning");
        certificat.setFormateurNom("INPG — Direction pédagogique");
        certificat.assurerReferenceEtDate();
        certificat.setSignature("abc123");

        when(certificatService.findByReference(certificat.getReference())).thenReturn(Optional.of(certificat));
        when(signatureService.estAlteree(certificat)).thenReturn(true);

        ResponseEntity<CertificatVerificationDto> reponse = controller.verifier(certificat.getReference());

        assertThat(reponse.getBody().valide()).isFalse();
        assertThat(reponse.getBody().verifiable()).isTrue();
    }

    @Test
    @DisplayName("Certificat émis avant la mise en place des signatures : non vérifiable, mais pas invalide")
    void certificatSansSignatureEstNonVerifiable() {
        Etudiant etudiant = new Etudiant(null, "Ndiaye", "Aissatou", "770000000");
        Certificat certificat = new Certificat();
        certificat.setEtudiant(etudiant);
        certificat.setIntituleFormation("Introduction au Machine Learning");
        certificat.setFormateurNom("INPG — Direction pédagogique");
        certificat.assurerReferenceEtDate();
        certificat.setSignature(null);

        when(certificatService.findByReference(certificat.getReference())).thenReturn(Optional.of(certificat));

        ResponseEntity<CertificatVerificationDto> reponse = controller.verifier(certificat.getReference());

        assertThat(reponse.getBody().verifiable()).isFalse();
        assertThat(reponse.getBody().valide()).isFalse();
    }
}
