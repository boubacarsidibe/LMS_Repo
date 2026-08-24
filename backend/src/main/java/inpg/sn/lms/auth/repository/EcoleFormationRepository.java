package inpg.sn.lms.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import inpg.sn.lms.auth.entity.EcoleFormation;

@Repository
public interface EcoleFormationRepository extends JpaRepository<EcoleFormation, Long> {

    Optional<EcoleFormation> findByUserId(Long userId);
}
