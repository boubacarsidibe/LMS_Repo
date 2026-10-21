package inpg.sn.lms.accreditation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import inpg.sn.lms.accreditation.dto.AttestationAccreditationVerificationDto;
import inpg.sn.lms.accreditation.entity.DemandeAccreditation;
import inpg.sn.lms.accreditation.enums.EnumNiveauProgramme;
import inpg.sn.lms.accreditation.enums.EnumStatutDemandeAccreditation;
import inpg.sn.lms.accreditation.service.AttestationAccreditationService;
import inpg.sn.lms.accreditation.service.AttestationAccreditationSignatureService;
import inpg.sn.lms.auth.entity.EcoleFormation;
import inpg.sn.lms.auth.entity.User;

/**
 * Vérification publique d'une attestation d'accréditation.
 *
 * <p>Mêmes garanties que {@code CertificatPublicControllerTest} : une
 * référence inconnue renvoie 404 sans détail, et la réponse n'expose jamais
 * l'adresse, le téléphone ni le plan d'actions d'une accréditation sous
 * réserve — seules les informations déjà imprimées sur le document.
 */
@ExtendWith(MockitoExtension.class)
class AttestationAccreditationPublicControllerTest {

    @Mock
    private AttestationAccreditationService attestationService;

    @Mock
    private AttestationAccreditationSignatureService signatureService;

    private AttestationAccreditationPublicController controller;

    @BeforeEach
    void setUp() {
        controller = new AttestationAccreditationPublicController(attestationService, signatureService);
    }

    private DemandeAccreditation demandeAccreditee() {
        User ecoleUser = new User();
        ecoleUser.setId(1L);
        EcoleFormation ecole = new EcoleFormation(ecoleUser, "Institut Supérieur du Pétrole", "Adresse secrète",
                "770000000", null);

        DemandeAccreditation demande = new DemandeAccreditation(ecole);
        demande.setIntituleProgramme("Technicien en forage");
        demande.setNiveauProgramme(EnumNiveauProgramme.TECHNICIEN);
        demande.setStatut(EnumStatutDemandeAccreditation.ACCREDITE);
        demande.setDateDecision(LocalDateTime.of(2026, 9, 27, 10, 0));
        demande.setDateExpiration(LocalDate.of(2029, 9, 27));
        demande.setReferenceAttestation("INPG-ACC-2026-ABCDEF01");
        demande.setSignatureAttestation("abc123");
        return demande;
    }

    @Test
    @DisplayName("Référence inconnue : 404, sans exposer d'information")
    void referenceInconnueRenvoie404() {
        when(attestationService.findByReference("INPG-ACC-2026-INCONNUE")).thenReturn(Optional.empty());

        ResponseEntity<AttestationAccreditationVerificationDto> reponse = controller.verifier("INPG-ACC-2026-INCONNUE");

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(reponse.getBody()).isNull();
    }

    @Test
    @DisplayName("Attestation valide : la réponse est cohérente et ne fuite ni l'adresse ni le téléphone")
    void attestationValideRenvoieLesChampsPublics() {
        DemandeAccreditation demande = demandeAccreditee();
        when(attestationService.findByReference(demande.getReferenceAttestation())).thenReturn(Optional.of(demande));
        when(signatureService.estAlteree(demande)).thenReturn(false);

        ResponseEntity<AttestationAccreditationVerificationDto> reponse = controller.verifier(demande.getReferenceAttestation());

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        AttestationAccreditationVerificationDto corps = reponse.getBody();
        assertThat(corps).isNotNull();
        assertThat(corps.reference()).isEqualTo(demande.getReferenceAttestation());
        assertThat(corps.nomEcole()).isEqualTo("Institut Supérieur du Pétrole");
        assertThat(corps.sousReserve()).isFalse();
        assertThat(corps.verifiable()).isTrue();
        assertThat(corps.valide()).isTrue();
        assertThat(corps.toString()).doesNotContain("Adresse secrète").doesNotContain("770000000");
    }

    @Test
    @DisplayName("Accréditation sous réserve : le drapeau sousReserve est vrai, jamais le plan d'actions")
    void accreditationSousReserveEstSignalee() {
        DemandeAccreditation demande = demandeAccreditee();
        demande.setStatut(EnumStatutDemandeAccreditation.ACCREDITE_SOUS_RESERVE);
        demande.setPlanActions("Recruter un formateur supplémentaire.");
        when(attestationService.findByReference(demande.getReferenceAttestation())).thenReturn(Optional.of(demande));
        when(signatureService.estAlteree(demande)).thenReturn(false);

        AttestationAccreditationVerificationDto corps = controller.verifier(demande.getReferenceAttestation()).getBody();

        assertThat(corps).isNotNull();
        assertThat(corps.sousReserve()).isTrue();
        assertThat(corps.toString()).doesNotContain("Recruter un formateur");
    }

    @Test
    @DisplayName("Attestation dont le contenu a été altéré après signature : valide=false")
    void attestationaltereeEstSignaleeInvalide() {
        DemandeAccreditation demande = demandeAccreditee();
        when(attestationService.findByReference(demande.getReferenceAttestation())).thenReturn(Optional.of(demande));
        when(signatureService.estAlteree(demande)).thenReturn(true);

        ResponseEntity<AttestationAccreditationVerificationDto> reponse = controller.verifier(demande.getReferenceAttestation());

        assertThat(reponse.getBody().valide()).isFalse();
        assertThat(reponse.getBody().verifiable()).isTrue();
    }
}
