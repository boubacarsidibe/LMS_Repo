package inpg.sn.lms.entreprise.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import inpg.sn.lms.entreprise.entity.FicheTechniqueModule;

@Repository
public interface FicheTechniqueModuleRepository extends JpaRepository<FicheTechniqueModule, Long> {

    Optional<FicheTechniqueModule> findByDemandeId(Long demandeId);
}
