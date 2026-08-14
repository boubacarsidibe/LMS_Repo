package inpg.sn.lms.espace_etudiant.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import inpg.sn.lms.espace_etudiant.entity.Certificat;

public interface CertificatRepository extends JpaRepository<Certificat, Long> {

    @Override
    @EntityGraph(attributePaths = { "etudiant", "cohorte" })
    List<Certificat> findAll();

    @Override
    @EntityGraph(attributePaths = { "etudiant", "cohorte" })
    Optional<Certificat> findById(Long id);

    @EntityGraph(attributePaths = { "etudiant", "cohorte" })
    List<Certificat> findByEtudiant_IdOrderByDateEmissionDesc(Long etudiantId);
}
