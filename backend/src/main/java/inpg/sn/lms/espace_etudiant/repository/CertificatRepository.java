package inpg.sn.lms.espace_etudiant.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import inpg.sn.lms.espace_etudiant.entity.Certificat;

public interface CertificatRepository extends JpaRepository<Certificat, Long> {

    @Override
    @EntityGraph(attributePaths = { "etudiant", "promotion" })
    List<Certificat> findAll();

    @Override
    @EntityGraph(attributePaths = { "etudiant", "promotion" })
    Optional<Certificat> findById(Long id);

    @EntityGraph(attributePaths = { "etudiant", "promotion" })
    List<Certificat> findByEtudiant_IdOrderByDateEmissionDesc(Long etudiantId);

    @EntityGraph(attributePaths = { "etudiant", "promotion" })
    Optional<Certificat> findByReference(String reference);
}
