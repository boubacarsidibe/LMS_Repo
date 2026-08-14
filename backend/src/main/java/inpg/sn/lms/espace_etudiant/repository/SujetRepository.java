package inpg.sn.lms.espace_etudiant.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import inpg.sn.lms.espace_etudiant.entity.Sujet;

public interface SujetRepository extends JpaRepository<Sujet, Long> {

    @Override
    @EntityGraph(attributePaths = "messages")
    List<Sujet> findAll();

    @Override
    @EntityGraph(attributePaths = "messages")
    java.util.Optional<Sujet> findById(Long id);
}
