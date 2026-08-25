package inpg.sn.lms.entreprise.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import inpg.sn.lms.entreprise.entity.MessageEntreprise;

@Repository
public interface MessageEntrepriseRepository extends JpaRepository<MessageEntreprise, Long> {

    List<MessageEntreprise> findByEntrepriseIdOrderByDateCreationAsc(Long entrepriseId);
}
