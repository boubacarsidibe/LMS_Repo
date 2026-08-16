package inpg.sn.lms.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import inpg.sn.lms.auth.entity.Candidat;

public interface CandidatRepository extends JpaRepository<Candidat, Long> {

    Optional<Candidat> findByUserEmail(String email);

    @Query("SELECT c FROM Candidat c WHERE c.user.id = :userId")
    Optional<Candidat> findByUserId(@Param("userId") Long userId);
}
