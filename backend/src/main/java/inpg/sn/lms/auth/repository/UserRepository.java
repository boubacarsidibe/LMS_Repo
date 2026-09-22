package inpg.sn.lms.auth.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import inpg.sn.lms.auth.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update User u set u.sessionVersion = u.sessionVersion + 1 where u.id = :id")
    int invaliderSessions(@Param("id") Long id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update User u set u.password = :password, u.sessionVersion = u.sessionVersion + 1 where u.id = :id")
    int changerMotDePasse(@Param("id") Long id, @Param("password") String password);

    User findByEmail(String email);
    User findByUsername(String username);

    /** Utilisateurs portant au moins un des rôles donnés (ex. les trois rôles « personnel »). */
    List<User> findDistinctByRoles_CodeInOrderByUsernameAsc(List<String> codesRole);
}
