package inpg.sn.lms.recrutement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import inpg.sn.lms.recrutement.entity.FichierStocke;

@Repository
public interface FichierStockeRepository extends JpaRepository<FichierStocke, Long> {
}