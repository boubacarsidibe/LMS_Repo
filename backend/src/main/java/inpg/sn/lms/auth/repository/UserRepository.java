package inpg.sn.lms.auth.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import inpg.sn.lms.auth.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);
    User findByUsername(String username);

    /** Utilisateurs portant au moins un des rôles donnés (ex. les trois rôles « personnel »). */
    List<User> findDistinctByRoles_CodeInOrderByUsernameAsc(List<String> codesRole);
}
