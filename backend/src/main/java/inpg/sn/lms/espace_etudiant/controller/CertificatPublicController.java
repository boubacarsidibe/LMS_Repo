package inpg.sn.lms.espace_etudiant.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import inpg.sn.lms.auth.entity.Etudiant;
import inpg.sn.lms.espace_etudiant.dto.CertificatVerificationDto;
import inpg.sn.lms.espace_etudiant.entity.Certificat;
import inpg.sn.lms.espace_etudiant.service.CertificatPdfService;
import inpg.sn.lms.espace_etudiant.service.CertificatService;
import inpg.sn.lms.espace_etudiant.service.CertificatSignatureService;

/**
 * Verification publique d'un certificat a partir de sa reference, accessible
 * sans authentification (voir {@code SecurityConfig}, prefixe {@code
 * /api/certificats/public/**}) : c'est la page vers laquelle pointe le QR code
 * imprime sur le certificat, destinee a un tiers (employeur, etablissement)
 * qui n'a pas de compte sur la plateforme.
 *
 * <p>Suit le meme principe que {@code recrutement.controller.PublicController} :
 * un prefixe dedie explicitement autorise (jamais un joker large), et une DTO
 * volontairement restreinte plutot que l'entite ou la DTO authentifiee.
 */
@RestController
@RequestMapping("/api/certificats/public")
public class CertificatPublicController {

    private final CertificatService certificatService;
    private final CertificatSignatureService signatureService;

    public CertificatPublicController(CertificatService certificatService, CertificatSignatureService signatureService) {
        this.certificatService = certificatService;
        this.signatureService = signatureService;
    }

    @GetMapping("/verifier/{reference}")
    public ResponseEntity<CertificatVerificationDto> verifier(@PathVariable String reference) {
        Certificat certificat = certificatService.findByReference(reference).orElse(null);
        if (certificat == null) {
            return ResponseEntity.notFound().build();
        }

        boolean verifiable = certificat.getSignature() != null;
        boolean valide = verifiable && !signatureService.estAlteree(certificat);

        Etudiant etudiant = certificat.getEtudiant();
        String nomComplet = etudiant == null ? "" : (etudiant.getPrenom() + " " + etudiant.getNom()).trim();

        return ResponseEntity.ok(new CertificatVerificationDto(
                certificat.getReference(),
                nomComplet,
                certificat.getIntituleFormation(),
                CertificatPdfService.libellePeriode(certificat.getDateDebut(), certificat.getDateFin()),
                certificat.getDateEmission() == null ? null : certificat.getDateEmission().toString(),
                certificat.getFormateurNom(),
                certificat.getCodeCertificat(),
                verifiable,
                valide));
    }
}
