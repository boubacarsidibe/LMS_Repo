package inpg.sn.lms.espace_etudiant.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import inpg.sn.lms.espace_etudiant.entity.Annonce;

public interface AnnonceRepository extends JpaRepository<Annonce, Long> {

    @EntityGraph(attributePaths = "cohortes")
    List<Annonce> findAllByOrderByEpingleeDescDateCreationDesc();
}
