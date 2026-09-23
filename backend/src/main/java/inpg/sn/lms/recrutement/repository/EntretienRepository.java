package inpg.sn.lms.recrutement.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import inpg.sn.lms.recrutement.entity.Entretien;

@Repository
public interface EntretienRepository extends JpaRepository<Entretien, Long> {

    List<Entretien> findByCandidatId(Long candidatId);

    Optional<Entretien> findByCandidatIdAndDateHeure(Long candidatId, java.time.LocalDateTime dateHeure);
    List<Entretien> findByGroupeEntretienId(UUID groupeEntretienId);
}
