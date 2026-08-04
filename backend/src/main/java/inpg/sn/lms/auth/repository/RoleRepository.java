package inpg.sn.lms.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import inpg.sn.lms.auth.entity.Role;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    public Role findByCode(String code);
}
