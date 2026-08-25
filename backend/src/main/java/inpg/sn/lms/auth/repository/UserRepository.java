package inpg.sn.lms.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import inpg.sn.lms.auth.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);
    User findByUsername(String username);
    
}
