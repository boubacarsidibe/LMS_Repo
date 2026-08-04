package inpg.sn.lms.recrutement.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import inpg.sn.lms.recrutement.entity.NotificationCandidat;

@Repository
public interface NotificationCandidatRepository extends JpaRepository<NotificationCandidat, Long> {

    List<NotificationCandidat> findByCandidatIdOrderByDateCreationDesc(Long candidatId);

    List<NotificationCandidat> findByCandidatIdAndLuFalseOrderByDateCreationDesc(Long candidatId);

    long countByCandidatIdAndLuFalse(Long candidatId);
}