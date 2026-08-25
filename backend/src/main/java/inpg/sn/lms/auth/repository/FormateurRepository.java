package inpg.sn.lms.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import inpg.sn.lms.auth.entity.Formateur;
import java.util.Optional;

public interface FormateurRepository extends JpaRepository<Formateur,Long> {
    Optional<Formateur> findByUserId(Long userId);

    Object findByUser_Username(String username);

    /** Sert à détecter un conflit avant d'écrire : le téléphone porte une contrainte d'unicité. */
    Optional<Formateur> findByTelephone(String telephone);
}
