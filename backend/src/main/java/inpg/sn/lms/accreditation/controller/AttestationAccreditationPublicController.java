package inpg.sn.lms.accreditation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import inpg.sn.lms.accreditation.dto.AttestationAccreditationVerificationDto;
import inpg.sn.lms.accreditation.entity.DemandeAccreditation;
import inpg.sn.lms.accreditation.enums.EnumStatutDemandeAccreditation;
import inpg.sn.lms.accreditation.service.AttestationAccreditationService;
import inpg.sn.lms.accreditation.service.AttestationAccreditationSignatureService;

/**
 * Vérification publique d'une attestation d'accréditation à partir de sa
 * référence, accessible sans authentification (voir {@code SecurityConfig},
 * préfixe {@code /api/attestations-accreditation/public/**}) : c'est la page
 * vers laquelle pointe le QR code imprimé sur l'attestation, destinée à un
 * tiers (futur étudiant, entreprise partenaire) sans compte sur la plateforme.
 *
 * <p>Distinct de {@code AccreditationPublicController} (liste des écoles
 * accréditées, réservée aux comptes connectés depuis la décision produit du
 * 2026-08-25, voir {@code LOG_BLOQUANT.md}) : vérifier UN document déjà en main
 * par sa référence opaque n'expose aucune liste énumérable, contrairement à un
 * répertoire complet — même distinction que {@code CertificatPublicController}
 * pour les certificats de formation, déjà public sur ce même principe.
 */
@RestController
@RequestMapping("/api/attestations-accreditation/public")
public class AttestationAccreditationPublicController {

    private final AttestationAccreditationService attestationService;
    private final AttestationAccreditationSignatureService signatureService;

    public AttestationAccreditationPublicController(AttestationAccreditationService attestationService,
            AttestationAccreditationSignatureService signatureService) {
        this.attestationService = attestationService;
        this.signatureService = signatureService;
    }

    /**
     * {@code @Transactional} n'est pas facultatif ici : {@code DemandeAccreditation.ecole} est chargée
     * paresseusement (open-in-view=false dans ce module), et {@code nomEcole} n'est lu qu'après le retour
     * du repository — sans transaction ouverte, {@code LazyInitializationException}. Même famille de piège
     * que documentée pour {@code espace_etudiant} (CLAUDE.md §3).
     */
    @GetMapping("/verifier/{reference}")
    @Transactional(readOnly = true)
    public ResponseEntity<AttestationAccreditationVerificationDto> verifier(@PathVariable String reference) {
        DemandeAccreditation demande = attestationService.findByReference(reference).orElse(null);
        if (demande == null) {
            return ResponseEntity.notFound().build();
        }

        boolean verifiable = demande.getSignatureAttestation() != null;
        boolean valide = verifiable && !signatureService.estAlteree(demande);

        return ResponseEntity.ok(new AttestationAccreditationVerificationDto(
                demande.getReferenceAttestation(),
                demande.getEcole() != null ? demande.getEcole().getNomEcole() : null,
                demande.getIntituleProgramme(),
                demande.getNiveauProgramme() != null ? demande.getNiveauProgramme().name() : null,
                demande.getDateDecision(),
                demande.getDateExpiration(),
                demande.getStatut() == EnumStatutDemandeAccreditation.ACCREDITE_SOUS_RESERVE,
                verifiable,
                valide));
    }
}
