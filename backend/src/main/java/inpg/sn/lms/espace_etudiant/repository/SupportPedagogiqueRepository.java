package inpg.sn.lms.espace_etudiant.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import inpg.sn.lms.espace_etudiant.entity.SupportPedagogique;
import java.util.UUID;

public interface SupportPedagogiqueRepository extends JpaRepository<SupportPedagogique, UUID> {

}
