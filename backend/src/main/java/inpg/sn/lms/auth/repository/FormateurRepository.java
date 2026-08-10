package inpg.sn.lms.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import inpg.sn.lms.auth.entity.Formateur;
import java.util.Optional;

public interface FormateurRepository extends JpaRepository<Formateur,Long> {
    Optional<Formateur> findByUserId(Long userId);
}
