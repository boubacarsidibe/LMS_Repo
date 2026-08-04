package inpg.sn.lms.recrutement.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import inpg.sn.lms.recrutement.entity.PieceJustificative;

@Repository
public interface PieceJustificativeRepository extends JpaRepository<PieceJustificative, Long> {

    List<PieceJustificative> findByCandidatureId(Long candidatureId);

    void deleteByCandidatureId(Long candidatureId);
}