package inpg.sn.lms.recrutement.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import inpg.sn.lms.recrutement.entity.Program;

@Repository
public interface ProgramRepository extends JpaRepository<Program, Long> {

    List<Program> findByLevel(String level);

    List<Program> findByTitleContainingIgnoreCase(String title);
}