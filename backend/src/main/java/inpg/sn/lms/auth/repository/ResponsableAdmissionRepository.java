package inpg.sn.lms.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import inpg.sn.lms.auth.entity.ResponsableAdmission;


@Repository
public interface ResponsableAdmissionRepository extends JpaRepository<ResponsableAdmission, Long> {
    
}