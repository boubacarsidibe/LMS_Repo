package inpg.sn.lms.rendezvous.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import inpg.sn.lms.rendezvous.entity.CreneauDisponibilite;

public interface CreneauDisponibiliteRepository extends JpaRepository<CreneauDisponibilite, Long> {

    List<CreneauDisponibilite> findByPersonnelIdAndReserveFalseAndDateHeureDebutAfterOrderByDateHeureDebutAsc(
            Long personnelId, LocalDateTime maintenant);

    List<CreneauDisponibilite> findByPersonnelIdAndDateHeureDebutAfterOrderByDateHeureDebutAsc(
            Long personnelId, LocalDateTime maintenant);
}
