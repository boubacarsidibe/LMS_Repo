package inpg.sn.lms.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import inpg.sn.lms.auth.entity.Candidat;

public interface CandidatRepository extends JpaRepository<Candidat, Long> {

    Optional<Candidat> findByUserEmail(String email);
}
