package inpg.sn.lms.espace_etudiant.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import inpg.sn.lms.espace_etudiant.entity.Certificat;
import inpg.sn.lms.espace_etudiant.repository.CertificatRepository;

@Service
public class CertificatService {

    private final CertificatRepository certificatRepository;

    public CertificatService(CertificatRepository certificatRepository) {
        this.certificatRepository = certificatRepository;
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

    public Certificat save(Certificat certificat) {
        return certificatRepository.save(certificat);
    }
}
