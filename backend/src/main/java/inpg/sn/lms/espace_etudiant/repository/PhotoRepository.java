package inpg.sn.lms.espace_etudiant.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import inpg.sn.lms.espace_etudiant.entity.Photo;

public interface PhotoRepository extends JpaRepository<Photo, Long> {

    List<Photo> findAllByOrderByDateAjoutDesc();
}
