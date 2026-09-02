package inpg.sn.lms.recrutement.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import inpg.sn.lms.recrutement.entity.ReponseCritereCandidature;

public interface ReponseCritereCandidatureRepository extends JpaRepository<ReponseCritereCandidature, Long> {

    List<ReponseCritereCandidature> findByCandidatureId(Long candidatureId);
}
