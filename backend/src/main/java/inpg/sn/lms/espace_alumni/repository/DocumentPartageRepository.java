package inpg.sn.lms.espace_alumni.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import inpg.sn.lms.espace_alumni.entity.DocumentPartage;

@Repository
public interface DocumentPartageRepository extends JpaRepository<DocumentPartage, Long> {

    List<DocumentPartage> findAllByOrderByDateAjoutDesc();
}
