package inpg.sn.lms.espace_etudiant.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.espace_etudiant.entity.Certificat;

/**
 * Signature électronique des certificats.
 *
 * <p>Ce qui est vérifié ici tient en deux propriétés : la signature est
 * <strong>déterministe</strong> (rejouer le même contenu redonne la même
 * signature — c'est ce qui permet de vérifier un certificat sans le stocker
 * en clair), et elle est <strong>sensible</strong> à toute modification d'un
 * champ imprimé sur le document (sans quoi elle ne détecterait rien).
 *
 * <p>Sans ce test, une régression rendrait la vérification publique inutile
 * en silence : soit toute donnée validerait (signature qui ignore son
 * contenu), soit plus rien ne validerait jamais (clé ou canonicalisation
 * incohérente entre la délivrance et la vérification).
 */
class CertificatSignatureServiceTest {

    private static final String CLE = "cle-de-test-suffisamment-longue";

    private CertificatSignatureService service;
    private Certificat certificat;

    @BeforeEach
    void setUp() {
        service = new CertificatSignatureService(CLE, "secret-jwt-de-test-suffisamment-long");

        Etudiant etudiant = new Etudiant(null, "Ndiaye", "Aissatou", "770000000");

        certificat = new Certificat();
        certificat.setEtudiant(etudiant);
        certificat.setIntituleFormation("Introduction au Machine Learning");
        certificat.setFormateurNom("INPG — Direction pédagogique");
        certificat.assurerReferenceEtDate();
    }

    @Test
    @DisplayName("Signer deux fois le même certificat produit la même signature")
    void signatureDeterministe() {
        String signature1 = service.signer(certificat);
        String signature2 = service.signer(certificat);

        assertThat(signature1).isNotBlank().hasSize(64);
        assertThat(signature1).isEqualTo(signature2);
    }

    @Test
    @DisplayName("Un certificat fraîchement signé n'est pas altéré")
    void certificatFraichementSigneEstValide() {
        certificat.setSignature(service.signer(certificat));

        assertThat(service.estAlteree(certificat)).isFalse();
    }

    @Test
    @DisplayName("Modifier le nom de l'étudiant après signature invalide la signature")
    void modificationApresSignatureEstDetectee() {
        certificat.setSignature(service.signer(certificat));

        certificat.getEtudiant().setNom("Diallo");

        assertThat(service.estAlteree(certificat)).isTrue();
    }

    @Test
    @DisplayName("Modifier l'intitulé de la formation après signature invalide la signature")
    void modificationIntituleEstDetectee() {
        certificat.setSignature(service.signer(certificat));

        certificat.setIntituleFormation("Autre formation");

        assertThat(service.estAlteree(certificat)).isTrue();
    }

    @Test
    @DisplayName("Un certificat sans signature n'est pas considéré altéré (non vérifiable, pas invalide)")
    void certificatSansSignatureNestPasAltere() {
        certificat.setSignature(null);

        assertThat(service.estAlteree(certificat)).isFalse();
    }

    @Test
    @DisplayName("Deux clés différentes produisent des signatures différentes pour le même contenu")
    void clesDifferentesProduisentDesSignaturesDifferentes() {
        CertificatSignatureService autreService = new CertificatSignatureService("une-autre-cle-tout-a-fait-differente", "secret-jwt-de-test-suffisamment-long");

        assertThat(service.signer(certificat)).isNotEqualTo(autreService.signer(certificat));
    }

    @Test
    @DisplayName("Une clé dédiée vide (CERTIFICAT_SIGNATURE_SECRET= copiée de .env.example) retombe sur JWT_SECRET au lieu de planter « Empty key »")
    void cleDedieeVideRetombeSurLeSecretJwt() {
        CertificatSignatureService sansCleDediee = new CertificatSignatureService("", CLE);

        assertThat(sansCleDediee.signer(certificat)).isEqualTo(service.signer(certificat));
    }

    @Test
    @DisplayName("La date d'émission ne change pas une fois fixée par assurerReferenceEtDate")
    void dateEtReferenceStablesApresGeneration() {
        LocalDate dateInitiale = certificat.getDateEmission();
        String referenceInitiale = certificat.getReference();

        certificat.assurerReferenceEtDate();

        assertThat(certificat.getDateEmission()).isEqualTo(dateInitiale);
        assertThat(certificat.getReference()).isEqualTo(referenceInitiale);
    }

    @Test
    @DisplayName("Un certificat sans champ du modèle INPG garde le contenu scellé d'origine : les anciennes signatures restent valides")
    void anciensCertificatsGardentLeurSignature() {
        String avantAjoutDesChamps = service.signer(certificat);
        certificat.setSignature(avantAjoutDesChamps);

        assertThat(service.estAlteree(certificat)).isFalse();
    }

    @Test
    @DisplayName("Les champs du modèle INPG sont scellés : modifier la période, le code, la civilité ou la naissance les invalide")
    void champsDuModeleInpgSontScelles() {
        certificat.setCivilite("Monsieur");
        certificat.setDateNaissance(LocalDate.of(1985, 8, 24));
        certificat.setDateDebut(LocalDate.of(2026, 2, 2));
        certificat.setDateFin(LocalDate.of(2026, 2, 6));
        certificat.setCodeCertificat("CG1001260206/03");
        certificat.setSignature(service.signer(certificat));
        assertThat(service.estAlteree(certificat)).isFalse();

        certificat.setDateFin(LocalDate.of(2026, 2, 7));
        assertThat(service.estAlteree(certificat)).as("période").isTrue();
        certificat.setDateFin(LocalDate.of(2026, 2, 6));

        certificat.setCodeCertificat("CG1001260206/04");
        assertThat(service.estAlteree(certificat)).as("code").isTrue();
        certificat.setCodeCertificat("CG1001260206/03");

        certificat.setCivilite("Madame");
        assertThat(service.estAlteree(certificat)).as("civilité").isTrue();
        certificat.setCivilite("Monsieur");

        certificat.setDateNaissance(LocalDate.of(1985, 8, 25));
        assertThat(service.estAlteree(certificat)).as("naissance").isTrue();
    }

    @Test
    @DisplayName("La date de délivrance est le jour de création (heure de Dakar), jamais une date déjà présente écrasée")
    void dateDeDelivranceEstLeJourDeCreation() {
        Certificat neuf = new Certificat();
        neuf.assurerReferenceEtDate();
        assertThat(neuf.getDateEmission()).isEqualTo(LocalDate.now(java.time.ZoneId.of("Africa/Dakar")));

        Certificat existant = new Certificat();
        existant.setDateEmission(LocalDate.of(2026, 9, 7));
        existant.assurerReferenceEtDate();
        assertThat(existant.getDateEmission()).isEqualTo(LocalDate.of(2026, 9, 7));
    }
}
