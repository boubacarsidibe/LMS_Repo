package inpg.sn.lms.espace_etudiant.controller;

import java.util.regex.Pattern;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import inpg.sn.lms.espace_etudiant.dto.CertificatVerificationDto;
import inpg.sn.lms.espace_etudiant.entity.Certificat;
import inpg.sn.lms.espace_etudiant.service.CertificatService;

/**
 * Vérification publique d'un certificat, accessible sans authentification en
 * flashant le QR code imprimé dessus (cf. {@code CertificatPdfService}).
 *
 * <p>Volontairement séparé de {@link EspaceEtudiantController} : ce dernier ne
 * sert que des rôles authentifiés (cf. {@code SecurityConfig}), alors que cet
 * endpoint doit rester joignable par n'importe qui, y compris un tiers qui
 * contrôle l'authenticité d'un diplôme sans compte sur la plateforme.
 *
 * <p>La réponse ({@link CertificatVerificationDto}) est délibérément plus
 * restreinte que {@code CertificatDto} : ni identifiant technique, ni matricule
 * ne sont exposés à un appelant anonyme.
 */
@RestController
@RequestMapping("/api/verification-certificat")
public class VerificationCertificatController {

    /** Format figé par {@code Certificat#genererReferenceSiAbsente} : rejeter tout le reste évite une requête inutile. */
    private static final Pattern FORMAT_REFERENCE = Pattern.compile("^INPG-CERT-\\d{4}-[0-9A-F]{8}$");

    private final CertificatService certificatService;

    public VerificationCertificatController(CertificatService certificatService) {
        this.certificatService = certificatService;
    }

    @Transactional(readOnly = true)
    @GetMapping("/{reference}")
    public ResponseEntity<CertificatVerificationDto> verifier(@PathVariable String reference) {
        if (reference == null || !FORMAT_REFERENCE.matcher(reference).matches()) {
            return ResponseEntity.notFound().build();
        }

        Certificat certificat = certificatService.findByReference(reference).orElse(null);
        if (certificat == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(toDto(certificat));
    }

    private CertificatVerificationDto toDto(Certificat certificat) {
        String etudiantNom = String.join(" ",
                certificat.getEtudiant().getPrenom(), certificat.getEtudiant().getNom()).trim();
        return new CertificatVerificationDto(
                certificat.getReference(),
                etudiantNom,
                certificat.getIntituleFormation(),
                certificat.getPromotion() != null ? certificat.getPromotion().getLibelle() : null,
                certificat.getDateEmission().toString(),
                certificat.getFormateurNom());
    }
}
