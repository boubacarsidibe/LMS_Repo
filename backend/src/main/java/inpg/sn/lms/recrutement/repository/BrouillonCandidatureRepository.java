package inpg.sn.lms.recrutement.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import inpg.sn.lms.recrutement.entity.BrouillonCandidature;

@Repository
public interface BrouillonCandidatureRepository extends JpaRepository<BrouillonCandidature, Long> {

    @Query("SELECT b FROM BrouillonCandidature b WHERE b.user.id = :userId")
    Optional<BrouillonCandidature> findByUserId(@Param("userId") Long userId);
}
