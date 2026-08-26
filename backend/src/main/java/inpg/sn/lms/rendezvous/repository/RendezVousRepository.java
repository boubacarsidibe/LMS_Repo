package inpg.sn.lms.rendezvous.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import inpg.sn.lms.rendezvous.entity.RendezVous;

public interface RendezVousRepository extends JpaRepository<RendezVous, Long> {

    List<RendezVous> findByDemandeurIdOrderByDateCreationDesc(Long demandeurId);

    List<RendezVous> findByCreneau_Personnel_IdOrderByDateCreationDesc(Long personnelId);
}
