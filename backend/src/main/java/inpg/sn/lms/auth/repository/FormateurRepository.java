package inpg.sn.lms.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import inpg.sn.lms.auth.entity.Formateur;

public interface FormateurRepository extends JpaRepository<Formateur,Long> {
    
}
