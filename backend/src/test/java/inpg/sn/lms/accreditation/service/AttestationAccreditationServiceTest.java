package inpg.sn.lms.accreditation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import inpg.sn.lms.accreditation.entity.DemandeAccreditation;
import inpg.sn.lms.accreditation.enums.EnumStatutDemandeAccreditation;
import inpg.sn.lms.accreditation.repository.DemandeAccreditationRepository;
import inpg.sn.lms.auth.entity.EcoleFormation;
import inpg.sn.lms.auth.entity.User;

/**
 * Référence et signature de l'attestation : générées seulement pour une
 * décision favorable (ACCREDITE / ACCREDITE_SOUS_RESERVE), jamais pour
 * SOUMISE, EN_COURS_EVALUATION ou REJETE — une demande rejetée qui obtiendrait
 * une référence deviendrait vérifiable publiquement, ce qui n'a pas de sens.
 */
class AttestationAccreditationServiceTest {

    private AttestationAccreditationService service;

    private DemandeAccreditation demande(EnumStatutDemandeAccreditation statut) {
        User ecoleUser = new User();
        ecoleUser.setId(1L);
        EcoleFormation ecole = new EcoleFormation(ecoleUser, "École X", "Adresse", "77000000", null);
        DemandeAccreditation demande = new DemandeAccreditation(ecole);
        demande.setIntituleProgramme("Technicien en forage");
        demande.setStatut(statut);
        return demande;
    }

    @BeforeEach
    void setUp() {
        DemandeAccreditationRepository repository = mock(DemandeAccreditationRepository.class);
        AttestationAccreditationSignatureService signatureService =
                new AttestationAccreditationSignatureService("cle-de-test-suffisamment-longue", "secret-jwt-de-test-suffisamment-long");
        service = new AttestationAccreditationService(repository, signatureService);
    }

    @Test
    @DisplayName("ACCREDITE reçoit une référence et une signature")
    void accrediteRecoitReferenceEtSignature() {
        DemandeAccreditation demande = demande(EnumStatutDemandeAccreditation.ACCREDITE);

        service.genererReferenceEtSignatureSiAccreditee(demande);

        assertThat(demande.getReferenceAttestation()).isNotBlank().startsWith("INPG-ACC-");
        assertThat(demande.getSignatureAttestation()).isNotBlank().hasSize(64);
    }

    @Test
    @DisplayName("ACCREDITE_SOUS_RESERVE reçoit aussi une référence et une signature")
    void accrediteSousReserveRecoitReferenceEtSignature() {
        DemandeAccreditation demande = demande(EnumStatutDemandeAccreditation.ACCREDITE_SOUS_RESERVE);

        service.genererReferenceEtSignatureSiAccreditee(demande);

        assertThat(demande.getReferenceAttestation()).isNotBlank();
        assertThat(demande.getSignatureAttestation()).isNotBlank();
    }

    @Test
    @DisplayName("SOUMISE, EN_COURS_EVALUATION et REJETE ne reçoivent jamais de référence")
    void statutsNonFavorablesNeRecoiventRien() {
        for (EnumStatutDemandeAccreditation statut : new EnumStatutDemandeAccreditation[] {
                EnumStatutDemandeAccreditation.SOUMISE, EnumStatutDemandeAccreditation.EN_COURS_EVALUATION,
                EnumStatutDemandeAccreditation.REJETE }) {
            DemandeAccreditation demande = demande(statut);

            service.genererReferenceEtSignatureSiAccreditee(demande);

            assertThat(demande.getReferenceAttestation()).as("statut " + statut).isNull();
            assertThat(demande.getSignatureAttestation()).as("statut " + statut).isNull();
        }
    }

    @Test
    @DisplayName("Idempotent : un second appel ne régénère pas une référence déjà présente")
    void idempotent() {
        DemandeAccreditation demande = demande(EnumStatutDemandeAccreditation.ACCREDITE);
        service.genererReferenceEtSignatureSiAccreditee(demande);
        String reference = demande.getReferenceAttestation();
        String signature = demande.getSignatureAttestation();

        service.genererReferenceEtSignatureSiAccreditee(demande);

        assertThat(demande.getReferenceAttestation()).isEqualTo(reference);
        assertThat(demande.getSignatureAttestation()).isEqualTo(signature);
    }

    @Test
    @DisplayName("findByReference délègue au repository")
    void findByReferenceDelegueAuRepository() {
        DemandeAccreditationRepository repository = mock(DemandeAccreditationRepository.class);
        AttestationAccreditationSignatureService signatureService =
                new AttestationAccreditationSignatureService("cle-de-test-suffisamment-longue", "secret-jwt-de-test-suffisamment-long");
        AttestationAccreditationService service = new AttestationAccreditationService(repository, signatureService);
        DemandeAccreditation demande = demande(EnumStatutDemandeAccreditation.ACCREDITE);
        when(repository.findByReferenceAttestation("INPG-ACC-2026-ABCDEF01"))
                .thenReturn(java.util.Optional.of(demande));

        assertThat(service.findByReference("INPG-ACC-2026-ABCDEF01")).contains(demande);
    }
}
