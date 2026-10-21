package inpg.sn.lms.accreditation.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import inpg.sn.lms.accreditation.entity.DemandeAccreditation;
import inpg.sn.lms.accreditation.enums.EnumNiveauProgramme;
import inpg.sn.lms.accreditation.enums.EnumStatutDemandeAccreditation;
import inpg.sn.lms.auth.entity.EcoleFormation;
import inpg.sn.lms.auth.entity.User;

/**
 * Signature électronique de l'attestation d'accréditation, même principe que
 * {@code CertificatSignatureServiceTest} : déterministe (nécessaire pour
 * vérifier sans stocker le contenu en clair une deuxième fois) et sensible à
 * toute modification d'un champ imprimé sur le document.
 */
class AttestationAccreditationSignatureServiceTest {

    private static final String CLE = "cle-de-test-suffisamment-longue";

    private AttestationAccreditationSignatureService service;
    private DemandeAccreditation demande;

    @BeforeEach
    void setUp() {
        service = new AttestationAccreditationSignatureService(CLE, "secret-jwt-de-test-suffisamment-long");

        User ecoleUser = new User();
        ecoleUser.setId(1L);
        EcoleFormation ecole = new EcoleFormation(ecoleUser, "École X", "Adresse", "77000000", null);

        demande = new DemandeAccreditation(ecole);
        demande.setIntituleProgramme("Technicien en forage");
        demande.setNiveauProgramme(EnumNiveauProgramme.TECHNICIEN);
        demande.setStatut(EnumStatutDemandeAccreditation.ACCREDITE);
        demande.setDateDecision(LocalDateTime.of(2026, 9, 27, 10, 0));
        demande.setDateExpiration(LocalDate.of(2029, 9, 27));
        demande.setReferenceAttestation("INPG-ACC-2026-ABCDEF01");
    }

    @Test
    @DisplayName("Signer deux fois la même demande produit la même signature")
    void signatureDeterministe() {
        String signature1 = service.signer(demande);
        String signature2 = service.signer(demande);

        assertThat(signature1).isNotBlank().hasSize(64);
        assertThat(signature1).isEqualTo(signature2);
    }

    @Test
    @DisplayName("Une attestation fraîchement signée n'est pas altérée")
    void fraichementSigneeEstValide() {
        demande.setSignatureAttestation(service.signer(demande));

        assertThat(service.estAlteree(demande)).isFalse();
    }

    @Test
    @DisplayName("Modifier le nom de l'école après signature invalide la signature")
    void modificationEcoleEstDetectee() {
        demande.setSignatureAttestation(service.signer(demande));

        demande.getEcole().setNomEcole("École Y");

        assertThat(service.estAlteree(demande)).isTrue();
    }

    @Test
    @DisplayName("Modifier l'intitulé du programme après signature invalide la signature")
    void modificationProgrammeEstDetectee() {
        demande.setSignatureAttestation(service.signer(demande));

        demande.setIntituleProgramme("Autre programme");

        assertThat(service.estAlteree(demande)).isTrue();
    }

    @Test
    @DisplayName("Basculer de ACCREDITE à ACCREDITE_SOUS_RESERVE après signature l'invalide")
    void modificationStatutEstDetectee() {
        demande.setSignatureAttestation(service.signer(demande));

        demande.setStatut(EnumStatutDemandeAccreditation.ACCREDITE_SOUS_RESERVE);

        assertThat(service.estAlteree(demande)).isTrue();
    }

    @Test
    @DisplayName("Repousser la date d'expiration après signature l'invalide")
    void modificationExpirationEstDetectee() {
        demande.setSignatureAttestation(service.signer(demande));

        demande.setDateExpiration(LocalDate.of(2030, 1, 1));

        assertThat(service.estAlteree(demande)).isTrue();
    }

    @Test
    @DisplayName("Une attestation sans signature n'est pas considérée altérée (non vérifiable, pas invalide)")
    void sansSignatureNestPasAlteree() {
        demande.setSignatureAttestation(null);

        assertThat(service.estAlteree(demande)).isFalse();
    }

    @Test
    @DisplayName("Deux secondes/nanosecondes différentes dans la même minute produisent la même signature "
            + "(faux positif d'altération détecté en vérification réelle après un aller-retour en base : "
            + "LocalDateTime.toString() varie selon que les secondes sont nulles ou non)")
    void precisionSousLaMinuteNInfluencePasLaSignature() {
        demande.setDateDecision(LocalDateTime.of(2026, 9, 27, 10, 0, 0, 0));
        String signatureSansSecondes = service.signer(demande);

        demande.setDateDecision(LocalDateTime.of(2026, 9, 27, 10, 0, 42, 123_456_000));
        String signatureAvecSecondesEtNanos = service.signer(demande);

        assertThat(signatureSansSecondes).isEqualTo(signatureAvecSecondesEtNanos);
    }

    @Test
    @DisplayName("Deux clés différentes produisent des signatures différentes pour le même contenu")
    void clesDifferentesProduisentDesSignaturesDifferentes() {
        AttestationAccreditationSignatureService autreService =
                new AttestationAccreditationSignatureService("une-autre-cle-tout-a-fait-differente", "secret-jwt-de-test-suffisamment-long");

        assertThat(service.signer(demande)).isNotEqualTo(autreService.signer(demande));
    }
}
