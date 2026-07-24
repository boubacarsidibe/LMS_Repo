package inpg.sn.lms.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import inpg.sn.lms.auth.entity.ResposablePedaEntity;


@Repository
public interface ResposablePedaRepository extends JpaRepository<ResposablePedaEntity, Long> {
    
}
