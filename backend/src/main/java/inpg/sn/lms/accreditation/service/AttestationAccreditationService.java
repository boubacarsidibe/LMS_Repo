package inpg.sn.lms.accreditation.service;

import java.time.Year;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import inpg.sn.lms.accreditation.entity.DemandeAccreditation;
import inpg.sn.lms.accreditation.enums.EnumStatutDemandeAccreditation;
import inpg.sn.lms.accreditation.repository.DemandeAccreditationRepository;

/**
 * Référence et signature de l'attestation d'accréditation (même principe que
 * {@code CertificatService} pour le certificat de formation).
 */
@Service
public class AttestationAccreditationService {

    private final DemandeAccreditationRepository demandeRepository;
    private final AttestationAccreditationSignatureService signatureService;

    public AttestationAccreditationService(DemandeAccreditationRepository demandeRepository,
            AttestationAccreditationSignatureService signatureService) {
        this.demandeRepository = demandeRepository;
        this.signatureService = signatureService;
    }

    /**
     * Fixe la référence et la signature au moment de la décision, seulement pour
     * une accréditation pleine ou sous réserve — jamais pour SOUMISE,
     * EN_COURS_EVALUATION ou REJETE. Un appel sur une demande qui a déjà les
     * siennes ne les régénère pas : {@link #trancher} n'appelle ceci qu'une
     * fois, mais l'idempotence évite une seconde signature divergente si jamais
     * ça changeait.
     */
    public void genererReferenceEtSignatureSiAccreditee(DemandeAccreditation demande) {
        boolean accreditee = demande.getStatut() == EnumStatutDemandeAccreditation.ACCREDITE
                || demande.getStatut() == EnumStatutDemandeAccreditation.ACCREDITE_SOUS_RESERVE;
        if (!accreditee || demande.getReferenceAttestation() != null) {
            return;
        }
        String suffixe = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        demande.setReferenceAttestation("INPG-ACC-" + Year.now().getValue() + "-" + suffixe);
        demande.setSignatureAttestation(signatureService.signer(demande));
    }

    public Optional<DemandeAccreditation> findByReference(String reference) {
        return demandeRepository.findByReferenceAttestation(reference);
    }
}
