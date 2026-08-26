package inpg.sn.lms.entreprise.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import inpg.sn.lms.entreprise.entity.DemandeEntreprise;

@Repository
public interface DemandeEntrepriseRepository extends JpaRepository<DemandeEntreprise, Long> {

    List<DemandeEntreprise> findByEntrepriseIdOrderByDateSoumissionDesc(Long entrepriseId);

    List<DemandeEntreprise> findAllByOrderByDateSoumissionDesc();
}
