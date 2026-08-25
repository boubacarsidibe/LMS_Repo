package inpg.sn.lms.espace_etudiant.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import inpg.sn.lms.espace_etudiant.entity.Certificat;
import inpg.sn.lms.espace_etudiant.repository.CertificatRepository;

@Service
public class CertificatService {

    private final CertificatRepository certificatRepository;
    private final CertificatSignatureService signatureService;

    public CertificatService(CertificatRepository certificatRepository, CertificatSignatureService signatureService) {
        this.certificatRepository = certificatRepository;
        this.signatureService = signatureService;
    }

    public List<Certificat> getAll() {
        return certificatRepository.findAll();
    }

    public List<Certificat> getByEtudiant(Long etudiantId) {
        return certificatRepository.findByEtudiant_IdOrderByDateEmissionDesc(etudiantId);
    }

    public Optional<Certificat> findById(Long id) {
        return certificatRepository.findById(id);
    }

    /**
     * Delivre le certificat : fixe reference/date si absentes puis calcule la
     * signature electronique sur leur valeur definitive, avant l'unique ecriture
     * en base.
     */
    public Certificat save(Certificat certificat) {
        certificat.assurerReferenceEtDate();
        certificat.setSignature(signatureService.signer(certificat));
        return certificatRepository.save(certificat);
    }

    public Optional<Certificat> findByReference(String reference) {
        return certificatRepository.findByReference(reference);
    }
}
