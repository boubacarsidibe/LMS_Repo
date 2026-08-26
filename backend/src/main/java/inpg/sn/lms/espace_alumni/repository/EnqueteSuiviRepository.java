package inpg.sn.lms.espace_alumni.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import inpg.sn.lms.espace_alumni.entity.EnqueteSuivi;

@Repository
public interface EnqueteSuiviRepository extends JpaRepository<EnqueteSuivi, Long> {

    List<EnqueteSuivi> findAllByOrderByDateCreationDesc();

    List<EnqueteSuivi> findByActiveTrueOrderByDateCreationDesc();
}
