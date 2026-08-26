package inpg.sn.lms.personnel.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import inpg.sn.lms.personnel.entity.Departement;

public interface DepartementRepository extends JpaRepository<Departement, Long> {

    List<Departement> findAllByOrderByLibelleAsc();

    List<Departement> findByActifTrueOrderByLibelleAsc();

    Optional<Departement> findByCode(String code);
}
