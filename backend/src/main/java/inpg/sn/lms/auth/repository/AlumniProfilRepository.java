package inpg.sn.lms.auth.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import inpg.sn.lms.auth.entity.AlumniProfil;

@Repository
public interface AlumniProfilRepository extends JpaRepository<AlumniProfil, Long> {
	Optional<AlumniProfil> findByUserId(Long userId);
}
