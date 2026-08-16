package inpg.sn.lms.espace_etudiant.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import inpg.sn.lms.espace_etudiant.entity.Photo;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, Long> {

    List<Photo> findByAlbumIdOrderByDateAjoutAsc(Long albumId);

    long countByAlbumId(Long albumId);
}
