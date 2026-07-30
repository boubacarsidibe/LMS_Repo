package inpg.sn.lms.recrutement.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import inpg.sn.lms.recrutement.entity.TestAcademique;

@Repository
public interface TestAcademiqueRepository extends JpaRepository<TestAcademique, Long> {

    List<TestAcademique> findByCandidatId(Long candidatId);
}